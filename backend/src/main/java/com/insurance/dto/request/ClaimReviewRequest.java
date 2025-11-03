package com.insurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reviewing a claim (approve or reject).
 * Sent by ADMIN or AGENT when processing a claim.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewRequest {

    @NotBlank(message = "Review notes are required")
    @Size(min = 10, max = 1000, message = "Review notes must be between 10 and 1000 characters")
    private String reviewNotes;
}
