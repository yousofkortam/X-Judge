package com.xjudge.controller.contest;

import com.xjudge.exception.UnauthenticatedException;
import com.xjudge.model.contest.ContestModel;
import com.xjudge.model.contest.ContestPageModel;
import com.xjudge.model.contest.ContestRankModel;
import com.xjudge.model.contest.ContestStatusPageModel;
import com.xjudge.model.contest.modification.ContestClientRequest;
import com.xjudge.model.problem.ProblemModel;
import com.xjudge.model.submission.SubmissionInfoModel;
import com.xjudge.model.submission.SubmissionModel;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xjudge.service.contest.ContestService;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/contest")
@Tag(name = "Contests", description = "The contest end-points for handling contest operations.")
public class ContestController {

    private final ContestService contestService;

    @GetMapping
    public  ResponseEntity<Page<ContestPageModel>> getAllContest(@RequestParam(defaultValue = "0") Integer pageNo,
                                                                  @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<ContestPageModel> contestPagesData = contestService.getAllContests(paging);
        return new ResponseEntity<>(contestPagesData , HttpStatus.OK);
    }

    @PreAuthorize(value="@contestSecurity.authorizeCreateContest(principal.username , #creationModel.groupId , #creationModel.type)")
    @PostMapping
    public ResponseEntity<ContestModel> createContest(@Valid @RequestBody ContestClientRequest creationModel, Authentication authentication) {
        return new ResponseEntity<>(contestService.createContest(creationModel , authentication) , HttpStatus.OK);
    }

    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username, #id , #password)")
    @GetMapping(value = "/{id}" )
    public ResponseEntity<ContestModel> getContest(@PathVariable("id") Long id , @RequestParam(defaultValue = "") String password) {
        return new ResponseEntity<>(contestService.getContestData(id) , HttpStatus.OK);
    }


    @PreAuthorize(value = "@contestSecurity.authorizeUpdate(principal.username , #id ,#model.type ,#model.groupId)")
    @PutMapping("/{id}")
    public ResponseEntity<ContestModel> updateContest(@PathVariable Long id
            , @Valid @RequestBody ContestClientRequest model
        , Authentication authentication) {
        if (authentication == null) {
            throw new UnauthenticatedException("You need to be authenticated to perform this operation");
        }
        return new ResponseEntity<>(contestService.updateContest(id, model , authentication) , HttpStatus.OK);

    }

    @PreAuthorize(value = "@contestSecurity.authorizeDelete(principal.username , #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContest(@PathVariable Long id) {
        contestService.deleteContest(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username , #id , #password)")
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmissionModel> submitContest(@PathVariable Long id, @Valid @RequestBody SubmissionInfoModel info, Authentication authentication ,  @RequestParam(defaultValue = "") String password) {
        SubmissionModel submission = contestService.submitInContest(id, info , authentication);
        return new ResponseEntity<>(submission , HttpStatus.OK);
    }

    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username , #id , #password)")
    @GetMapping("/{id}/problems")
    public ResponseEntity<List<ProblemModel>> getContestProblems(@PathVariable Long id , @RequestParam(defaultValue = "") String password){
        return new ResponseEntity<>(contestService.getContestProblems(id) , HttpStatus.OK);
    }

    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username , #id , #password)")
    @GetMapping("/{id}/problem/{problemHashtag}")
    public ResponseEntity<ProblemModel> getContestProblem(@PathVariable Long id, @PathVariable String problemHashtag ,  @RequestParam(defaultValue = "") String password){
        return new ResponseEntity<>(contestService.getContestProblem(id, problemHashtag) , HttpStatus.OK);
    }

    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username , #id , #password)")
    @GetMapping(path = "/{id}/submissions" , params = {"userHandle" ,  "problemCode" , "result" , "language"})
    public ResponseEntity<Page<ContestStatusPageModel>> getContestSubmissions(@PathVariable Long id ,
                                                                              @RequestParam(required = false ,defaultValue = "") String userHandle,
                                                                              @RequestParam(required = false ,defaultValue = "") String problemCode,
                                                                              @RequestParam(required = false ,defaultValue = "") String result,
                                                                              @RequestParam(required = false ,defaultValue = "") String language,
                                                                              Pageable pageable,
                                                                              @RequestParam(defaultValue = "") String password){
        return new ResponseEntity<>(contestService.getContestSubmissions(id,userHandle , problemCode,result , language , pageable) , HttpStatus.OK);
    }

    @ResponseStatus(code = HttpStatus.OK)
    @GetMapping("/{id}/rank")
    @PreAuthorize(value = "@contestSecurity.authorizeContestantsRoles(principal.username , #id , #password)")
    public ResponseEntity<List<ContestRankModel>> getRank(@PathVariable long id , @RequestParam(defaultValue = "") String password){
       return new ResponseEntity<>(contestService.getRank(id) , HttpStatus.OK);
    }


    @GetMapping("/search")
    public  ResponseEntity<Page<ContestPageModel>> searchByTitleAndOwner( @RequestParam(defaultValue = "" , required = false) String title ,
                                             @RequestParam(defaultValue = "" , required = false) String owner ,
                                             Pageable pageable) {
        Page<ContestPageModel> contestPageModels = contestService.searchContestByTitleOrOwner(title, owner, pageable);
        return new ResponseEntity<>(contestPageModels , HttpStatus.OK);
    }

    @GetMapping(params = {"status"})
    public  ResponseEntity<Page<ContestPageModel>> filterByStatus( @RequestParam(defaultValue = "RUNNING") String status, Pageable pageable) {
        return new ResponseEntity<>(contestService.getContestByStatus(status, pageable.getPageNumber(), pageable.getPageSize()) , HttpStatus.OK);
    }

    @GetMapping(params = {"type" , "status"})
    public ResponseEntity<Page<ContestPageModel>> getContestByType(@RequestParam(defaultValue = "CLASSIC") String type, @RequestParam(required = false , defaultValue = "") String status, Pageable pageable){
        return new ResponseEntity<>(contestService.getContestsByType(type , status , pageable) , HttpStatus.OK);
    }


    @GetMapping(params = {"visibility" , "status"})
    public ResponseEntity<Page<ContestPageModel>> getContestByVisibility(@RequestParam(defaultValue = "PUBLIC") String visibility , @RequestParam(required = false , defaultValue = "") String status ,
                                              Pageable pageable){
        return new ResponseEntity<>(contestService.getContestsByVisibility(visibility , status , pageable) , HttpStatus.OK);
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ContestPageModel>> getContestOfUser(Authentication authentication , @RequestParam(required = false , defaultValue = "") String status ,
                                                                   Pageable pageable){
        if (authentication == null) {
            throw new UnauthenticatedException("You need to be authenticated to perform this operation");
        }
        return new ResponseEntity<>(contestService.getContestsOfLoginUser(authentication , status , pageable) , HttpStatus.OK);
    }

    @GetMapping(params = {"category" , "status" , "title" , "owner"})
    public ResponseEntity<Page<ContestPageModel>> globalSearch(@RequestParam(defaultValue = "") String category, @RequestParam(required = false , defaultValue = "") String status ,
                                                    @RequestParam(defaultValue = "" , required = false) String title,
                                                    @RequestParam(defaultValue = "" , required = false) String owner,
                                                               Pageable pageable, Authentication authentication){
        if(category.equals("mine")) {
            if (authentication == null) {
                throw new UnauthenticatedException("You need to be authenticated to perform this operation");
            }
            category = authentication.getName();
        }
        return new ResponseEntity<>(contestService.searchByVisibilityOrTypeOrUserAndOwnerAndTitle(category , title , owner ,status, pageable) , HttpStatus.OK);
    }
}
