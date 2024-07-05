package com.xjudge.service.problem;

import com.xjudge.entity.Compiler;
import com.xjudge.entity.Problem;
import com.xjudge.entity.Submission;
import com.xjudge.entity.User;
import com.xjudge.exception.XJudgeException;
import com.xjudge.mapper.ProblemMapper;
import com.xjudge.mapper.SubmissionMapper;
import com.xjudge.model.enums.OnlineJudgeType;
import com.xjudge.model.problem.ProblemDescription;
import com.xjudge.model.problem.ProblemModel;
import com.xjudge.model.problem.ProblemsPageModel;
import com.xjudge.model.submission.SubmissionInfoModel;
import com.xjudge.model.submission.SubmissionModel;
import com.xjudge.model.user.Statistics;
import com.xjudge.repository.ProblemRepository;
import com.xjudge.service.compiler.CompilerService;
import com.xjudge.service.scraping.strategy.ScrappingStrategy;
import com.xjudge.service.scraping.strategy.SubmissionStrategy;
import com.xjudge.service.submission.SubmissionService;
import com.xjudge.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProblemServiceImp implements ProblemService {

    private final ProblemRepository problemRepo;
    private final Map<OnlineJudgeType, ScrappingStrategy> scrappingStrategies;
    private final Map<OnlineJudgeType, SubmissionStrategy> submissionStrategies;
    private final SubmissionService submissionService;
    private final SubmissionMapper submissionMapper;
    private final UserService userService;
    private final ProblemMapper problemMapper;
    private final CompilerService compilerService;

    @Override
    public Page<ProblemsPageModel> getAllProblems(Pageable pageable) {
        Page<Problem> problemList = problemRepo.findAll(pageable);
        return problemList.map(problemMapper::toPageModel);
    }

    @Override
    public Page<ProblemsPageModel> filterProblems(String source, String problemCode, String title, String contestName, Pageable pageable) {
        Page<Problem> problemList = problemRepo.filterProblems(source, problemCode, title, contestName, pageable);
        return problemList.map(problemMapper::toPageModel);
    }

    @Override
    @Transactional
    public Problem getProblem(String source, String code) {
        Optional<Problem> problem = problemRepo.findByCodeAndOnlineJudge(code, OnlineJudgeType.valueOf(source.toLowerCase()));
        return problem.orElseGet(() -> scrapProblem(source, code));
    }

    @Override
    @Transactional
    public ProblemModel getProblemModel(String source, String code) {
        return problemMapper.toModel(getProblem(source, code));
    }

    private Problem scrapProblem(String source, String code) {
        ScrappingStrategy strategy = scrappingStrategies.get(OnlineJudgeType.valueOf(source.toLowerCase()));
        Problem problem = strategy.scrap(code);
        return problemRepo.save(problem);
    }

    @Override
    @Transactional
    public ProblemDescription getProblemDescription(String source, String code) {
        Problem problem = problemRepo.findByCodeAndOnlineJudge(code, OnlineJudgeType.valueOf(source.toLowerCase()))
                .orElseThrow(
                        () -> new XJudgeException("Problem not found", ProblemServiceImp.class.getName(), HttpStatus.NOT_FOUND)
                );
        return problemMapper.toDescription(problem);
    }

    @Override
    @Transactional
    public Submission submit(SubmissionInfoModel info , Authentication authentication) {
        User user = userService.findUserByHandle(authentication.getName());
        Problem problem = getProblem(info.ojType().name(), info.code());
        Compiler compiler = compilerService.getCompilerByIdValue(info.compiler().getIdValue());
        SubmissionStrategy strategy = submissionStrategies.get(info.ojType());
        Submission submission = strategy.submit(info);
        submission.setProblem(problem);
        submission.setCompiler(compiler);
        user.setAttemptedCount(user.getAttemptedCount()+1);
        if(submission.getVerdict().equalsIgnoreCase("Accepted") && !hasUserSolvedProblem(user, problem)){
            user.setSolvedCount(user.getSolvedCount()+1);
            problem.setSolvedCount(problem.getSolvedCount()+1);
        }
        user = userService.save(user);
        submission.setUser(user);
        return submissionService.save(submission);
    }

    public boolean hasUserSolvedProblem(User user, Problem problem) {
        List<Submission> submissions = submissionService.findByUserAndProblem(user, problem);
        for (Submission submission : submissions) {
            if (submission.getVerdict().equalsIgnoreCase("Accepted")) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public SubmissionModel submitClient(SubmissionInfoModel info, Authentication authentication) {
        return submissionMapper.toModel(submit(info , authentication));
    }

    @Override
    public Page<ProblemsPageModel> searchByTitle(String title, Pageable pageable) {
        Page<Problem> problemList = problemRepo.findByTitleContaining(title, pageable);
        return problemList.map(problem -> problemMapper.toPageModel(
                problem, submissionService.getSolvedCount(problem.getCode(), OnlineJudgeType.codeforces))
        );
    }

    @Override
    public Page<ProblemsPageModel> searchBySource(String source, Pageable pageable) {
        Page<Problem> problemList = problemRepo.findByOnlineJudgeContaining(OnlineJudgeType.valueOf(source), pageable);
        return problemList.map(problem -> problemMapper.toPageModel(
                problem, submissionService.getSolvedCount(problem.getCode(), OnlineJudgeType.codeforces))
        );
    }

    @Override
    public Page<ProblemsPageModel> searchByProblemCode(String problemCode, Pageable pageable) {
        Page<Problem> problemList = problemRepo.findByCodeContaining(problemCode, pageable);
        return problemList.map(problem -> problemMapper.toPageModel(
                problem, submissionService.getSolvedCount(problem.getCode(), OnlineJudgeType.codeforces))
        );
    }

    @Override
    public Statistics getStatistics(Principal connectedUser) {
        User user = userService.findUserByHandle(connectedUser.getName());
        return new Statistics(user.getSolvedCount(), user.getAttemptedCount());
    }

}
