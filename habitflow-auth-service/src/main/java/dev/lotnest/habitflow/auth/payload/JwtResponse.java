package dev.lotnest.habitflow.auth.payload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String token;
}
