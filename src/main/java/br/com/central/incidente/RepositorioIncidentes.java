package br.com.central.incidente;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class RepositorioIncidentes {
    private final JdbcTemplate jdbc;
    public RepositorioIncidentes(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> listar() { return jdbc.queryForList("SELECT * FROM vw_incidentes_detalhados ORDER BY data_abertura DESC"); }
    public List<Map<String, Object>> resumo() { return jdbc.queryForList("SELECT * FROM vw_resumo_incidentes"); }
    public List<Map<String, Object>> detalhes(String id) { return jdbc.queryForList("SELECT * FROM vw_incidentes_detalhados WHERE id_incidente = ?", id); }
    public List<Map<String, Object>> acoes(String id) { return jdbc.queryForList("SELECT a.id_acao, a.id_autor, u.nome AS autor, a.descricao, a.conclusiva, a.data_acao FROM acoes_resposta a JOIN usuarios u ON u.id_usuario = a.id_autor WHERE a.id_incidente = ? ORDER BY a.data_acao", id); }
    public List<Map<String, Object>> tipos() { return jdbc.queryForList("SELECT id_tipo, nome FROM tipos_incidente ORDER BY nome"); }
    public List<Map<String, Object>> severidades() { return jdbc.queryForList("SELECT id_severidade, nome FROM severidades ORDER BY id_severidade"); }
    public List<Map<String, Object>> ativos(String id) { return jdbc.queryForList("SELECT a.id_ativo, a.nome, a.descricao FROM ativos a JOIN incidente_ativo ia ON ia.id_ativo = a.id_ativo WHERE ia.id_incidente = ?", id); }
    public List<Map<String, Object>> historico(String id) { return jdbc.queryForList("SELECT h.id_status_anterior, h.id_status_novo, h.id_autor, u.nome AS autor, h.data_alteracao FROM historico_status h JOIN usuarios u ON u.id_usuario = h.id_autor WHERE h.id_incidente = ? ORDER BY h.data_alteracao", id); }

    public String abrir(String titulo, String descricao, int tipo, int severidade, String autor) {
        return jdbc.execute((ConnectionCallback<String>) conn -> {
            try (PreparedStatement call = conn.prepareStatement("CALL sp_abrir_incidente(?, ?, ?, ?, ?, @incidente_criado)")) {
                call.setString(1, titulo); call.setString(2, descricao); call.setInt(3, tipo);
                call.setInt(4, severidade); call.setString(5, autor);
                call.execute();
            }
            try (Statement consulta = conn.createStatement();
                 ResultSet resultado = consulta.executeQuery("SELECT @incidente_criado")) {
                return resultado.next() ? resultado.getString(1) : null;
            }
        });
    }
    public void iniciar(String id, String autor) { jdbc.update("CALL sp_iniciar_analise(?, ?)", id, autor); }
    public void encerrar(String id, String autor, String descricao) { jdbc.update("CALL sp_encerrar_incidente(?, ?, ?)", id, autor, descricao); }
    public void registrarAcao(String id, String autor, String descricao) {
        int inseridas = jdbc.update("""
                INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
                SELECT fn_gerar_id_operacional('ACA'), id_incidente, ?, ?
                FROM vw_incidentes_detalhados WHERE id_incidente = ? AND status = 'EM ANÁLISE'
                """, autor, descricao, id);
        if (inseridas != 1) throw new IllegalStateException("Incidente inexistente ou não está em análise");
    }
}
