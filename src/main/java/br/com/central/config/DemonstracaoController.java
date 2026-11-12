package br.com.central.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@ConditionalOnProperty(name = "central.demonstracao", havingValue = "true")
public class DemonstracaoController {
    private final AuthenticationManager autenticador;
    private static final Map<String, String> PERFIS = Map.of(
            "administrador", "admin@exemplo.invalid",
            "analista", "analista@exemplo.invalid",
            "visualizador", "visualizador@exemplo.invalid");

    public DemonstracaoController(AuthenticationConfiguration config) throws Exception {
        this.autenticador = config.getAuthenticationManager();
    }

    @PostMapping("/demonstracao/entrar")
    public String entrar(@RequestParam String perfil, HttpServletRequest request, HttpServletResponse response) {
        String email = PERFIS.get(perfil);
        if (email == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            Authentication autenticacao = autenticador.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, "CentralDemo123!"));
            if (request.getSession(false) != null) request.changeSessionId();
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);
            new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
            return "redirect:/";
        } catch (AuthenticationException e) {
            return "redirect:/login?demoErro";
        }
    }
}

@ControllerAdvice
class DemonstracaoModelo {
    @Value("${central.demonstracao:false}")
    private boolean demonstracao;

    @ModelAttribute("demonstracao")
    boolean demonstracao() { return demonstracao; }
}
