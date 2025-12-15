package payetonkawa.api_commande.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> rolesList)) {
            return List.of();
        }

        // SANS préfixe ROLE_
        List<GrantedAuthority> authorities = rolesList.stream()
                .filter(role -> role instanceof String)
                .map(role -> new SimpleGrantedAuthority((String) role))
                .collect(Collectors.toList());

        System.out.println("🔐 Authorities extraites: " + authorities);
        return authorities;
    }
}
