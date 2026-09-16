package enterprise.ticket_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {
    @Value("classpath:keys/public_key.pem")
    private Resource publicKeyResource;

    @Bean
    public PublicKey publicKey() throws Exception {
        String key = readPemKey(publicKeyResource, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String readPemKey(Resource resource, String begin, String end) {
        try (var inputStream = resource.getInputStream()){
            String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return key
                    .replace(begin, "")
                    .replace(end, "")
                    .replaceAll("\\s+", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey)
                .build();
    }
}
