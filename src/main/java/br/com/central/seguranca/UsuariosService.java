package br.com.central.seguranca;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuariosService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public UsuariosService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return jdbc.query("""
                SELECT u.id_usuario, u.email, u.senha_hash, u.ativo, g.nome AS grupo
                FROM usuarios u JOIN grupos_usuarios g ON g.id_grupo = u.id_grupo
                WHERE u.email = ?
                """, (rs, n) -> new UsuarioAutenticado(rs.getString("id_usuario"), rs.getString("email"),
                rs.getString("senha_hash"), rs.getString("grupo"), rs.getBoolean("ativo")), email)
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
