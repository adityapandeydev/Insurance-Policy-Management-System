package com.insurance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                         API RESPONSE WRAPPER                            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Generic wrapper for ALL API responses. Ensures every endpoint          ║
 * ║  returns a consistent JSON structure, regardless of success or failure. ║
 * ║                                                                          ║
 * ║  SUCCESS RESPONSE:                                                       ║
 * ║  {                                                                       ║
 * ║    "success": true,                                                      ║
 * ║    "message": "Customer retrieved successfully",                        ║
 * ║    "data": { "id": 1, "firstName": "John", ... },                      ║
 * ║    "timestamp": "2024-05-15T10:30:00",                                 ║
 * ║    "statusCode": 200                                                    ║
 * ║  }                                                                       ║
 * ║                                                                          ║
 * ║  ERROR RESPONSE:                                                         ║
 * ║  {                                                                       ║
 * ║    "success": false,                                                     ║
 * ║    "message": "Customer not found with id: 42",                        ║
 * ║    "data": null,                                                         ║
 * ║    "timestamp": "2024-05-15T10:30:00",                                 ║
 * ║    "statusCode": 404                                                    ║
 * ║  }                                                                       ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: Why use a generic wrapper?                              ║
 * ║  1. Frontend can always check `success` field to handle errors          ║
 * ║  2. Consistent error parsing logic on the client side                  ║
 * ║  3. Timestamp and statusCode aid in debugging                           ║
 * ║  4. `data` field is type-safe via generics ApiResponse<CustomerDTO>    ║
 * ║                                                                          ║
 * ║  @JsonInclude(NON_NULL): `data: null` fields are omitted from JSON      ║
 * ║  for cleaner error responses.                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit null fields from JSON output
public class ApiResponse<T> {

    /** Whether the request was successful */
    private boolean success;

    /** Human-readable message describing the result */
    private String message;

    /** The response payload (null on error) */
    private T data;

    /** When this response was generated (UTC) */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** HTTP status code (200, 201, 400, 404, etc.) */
    private int statusCode;

    // ─── STATIC FACTORY METHODS ───────────────────────────────────────────
    // These provide a convenient API for creating responses without boilerplate.

    /**
     * Creates a successful response with data and message.
     *
     * @param message Description of what happened
     * @param data    The response payload
     * @param code    HTTP status code
     * @param <T>     Type of the payload
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(String message, T data, int code) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .statusCode(code)
                .build();
    }

    /**
     * Creates a successful response without data (e.g., for DELETE operations).
     */
    public static <T> ApiResponse<T> success(String message, int code) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .statusCode(code)
                .build();
    }

    /**
     * Creates an error response with a message and HTTP status code.
     * Data is null (omitted from JSON via @JsonInclude(NON_NULL)).
     */
    public static <T> ApiResponse<T> error(String message, int code) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .statusCode(code)
                .build();
    }
}
