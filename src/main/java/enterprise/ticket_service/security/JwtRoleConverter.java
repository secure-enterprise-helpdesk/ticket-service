package enterprise.ticket_service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");

        if (role == null || role.isBlank()) {
            return new JwtAuthenticationToken(jwt, List.of());
        }

        String normalizedRole = role.trim();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalizedRole);

        return new JwtAuthenticationToken(
                jwt,
                List.of(authority)
        );
    }
}
