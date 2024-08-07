package com.xjudge.model.group;

import com.xjudge.model.enums.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupRequest {
    @NotBlank(message = "Group name is required")
    @NotNull(message = "Group name is required")
    String name;

    String description;

    @NotNull(message = "Please enter a valid visibility")
    GroupVisibility visibility;
}
