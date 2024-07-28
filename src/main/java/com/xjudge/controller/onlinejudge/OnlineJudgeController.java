package com.xjudge.controller.onlinejudge;

import com.xjudge.model.enums.OnlineJudgeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "Compiler", description = "The end-points related to online judges we are working with.")
public class OnlineJudgeController {

    @GetMapping("/online-judge")
    public ResponseEntity<OnlineJudgeType[]> getOnlineJudgeList() {
        return ResponseEntity.ok(OnlineJudgeType.values());
    }

}
