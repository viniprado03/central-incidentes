package br.com.central.seguranca;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public class UsuarioAutenticado extends User {
    private final String id;

    public UsuarioAutenticado(String id, String email, String senha, String grupo, boolean ativo) {
        super(email, senha, ativo, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + grupo)));
        this.id = id;
    }

    public String getId() { return id; }
}
