package com.xjudge.controller.problem;

import com.xjudge.model.problem.ProblemModel;
import com.xjudge.model.problem.ProblemsPageModel;

import com.xjudge.model.submission.SubmissionInfoModel;
import com.xjudge.model.submission.SubmissionModel;
import com.xjudge.model.user.Statistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.xjudge.service.problem.ProblemService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@RestController
@RequiredArgsConstructor
@RequestMapping("problem")
@Tag(name = "Problem", description = "The Problem End-Points for fetching & submitting problems.")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "Retrieve all problems", description = "Get all problems available in the system with pagination.")
    public ResponseEntity<Page<ProblemsPageModel>> getAllProblems(@RequestParam(defaultValue = "0") Integer pageNo,
                                            @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ProblemsPageModel> paginatedData = problemService.getAllProblems(paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping(params = {"source", "problemCode", "title", "contestName"})
    public ResponseEntity<Page<ProblemsPageModel>> filterProblems(@RequestParam(defaultValue = "") String source,
                                            @RequestParam(defaultValue = "") String problemCode,
                                            @RequestParam(defaultValue = "") String title,
                                            @RequestParam(defaultValue = "") String contestName,
                                            @RequestParam(defaultValue = "0") Integer pageNo,
                                            @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ProblemsPageModel> paginatedData = problemService.filterProblems(source, problemCode, title, contestName, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping("/{source}-{code}")
    @Operation(summary = "Retrieve a specific problem", description = "Get a specific problem by its code.")
    public ResponseEntity<ProblemModel> getProblem(@PathVariable String source,
                                                   @PathVariable String code) {
        return new ResponseEntity<>(problemService.getProblemModel(source, code), HttpStatus.OK);
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a problem", description = "Submit a specific problem to be judged.")
    public ResponseEntity<SubmissionModel> submit(@Valid @RequestBody SubmissionInfoModel info, Authentication authentication){
        com.xjudge.util.Authentication.checkAuthentication(authentication);
        return new ResponseEntity<>(problemService.submitClient(info , authentication), HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search problems by title", description = "Search problems by their title.")
    public ResponseEntity<Page<ProblemsPageModel>> searchByTitle(@RequestParam String title, @RequestParam(defaultValue = "0") Integer pageNo,
                                           @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ProblemsPageModel> paginatedData = problemService.searchByTitle(title, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping("/source")
    @Operation(summary = "Search problems by source", description = "Search problems by their source.")
    public ResponseEntity<Page<ProblemsPageModel>> searchBySource(@RequestParam String source, @RequestParam(defaultValue = "0") Integer pageNo,
                                           @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ProblemsPageModel> paginatedData = problemService.searchBySource(source, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping("/code")
    @Operation(summary = "Search problems by problem code", description = "Search problems by their problem code.")
    public ResponseEntity<Page<ProblemsPageModel>> searchByProblemCode(@RequestParam String problemCode, @RequestParam(defaultValue = "0") Integer pageNo,
                                                @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ProblemsPageModel> paginatedData = problemService.searchByProblemCode(problemCode, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping("/user-statistics")
    public ResponseEntity<Statistics> getStatistics(Principal connectedUser) {
        com.xjudge.util.Authentication.checkAuthentication(connectedUser);
        return new ResponseEntity<>(problemService.getStatistics(connectedUser), HttpStatus.OK);
    }

}
