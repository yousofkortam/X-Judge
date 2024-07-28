package com.xjudge.controller.submission;

import com.xjudge.model.submission.SubmissionModel;
import com.xjudge.model.submission.SubmissionPageModel;
import com.xjudge.service.submission.SubmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/submission")
@Tag(name = "Submission", description = "The end-points related to submission operations.")
public class SubmissionController {
    private final SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<Page<SubmissionPageModel>> getAllSubmissions(@RequestParam(defaultValue = "0") Integer pageNo,
                                                                       @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        return new ResponseEntity<>(submissionService.getAllSubmissions(paging) , HttpStatus.OK);
    }

    @GetMapping(params = {"userHandle" , "oj" , "problemCode" , "language"})
    public ResponseEntity<Page<SubmissionPageModel>> filterSubmissions(@RequestParam(required = false ,defaultValue = "") String userHandle,
                                                      @RequestParam(required = false ,defaultValue = "") String oj,
                                                      @RequestParam(required = false ,defaultValue = "") String problemCode,
                                                      @RequestParam(required = false ,defaultValue = "") String language,
                                                      @RequestParam(defaultValue = "0") Integer pageNo,
                                                      @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        return new ResponseEntity<>(submissionService.filterSubmissions(userHandle, oj, problemCode, language, paging) , HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionModel> getSubmissionById(@PathVariable long id , Authentication authentication){
        return new ResponseEntity<>(submissionService.getSubmissionById(id , authentication) , HttpStatus.OK);
    }

    @PutMapping("/{id}/open")
    public ResponseEntity<Boolean> openSubmission(@PathVariable long id , Authentication authentication){
        com.xjudge.util.Authentication.checkAuthentication(authentication);
        return new ResponseEntity<>(submissionService.updateSubmissionOpen(id , authentication), HttpStatus.OK);
    }
}
