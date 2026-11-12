package br.com.central.incidente;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class IncidentesService {
    private final RepositorioIncidentes repo;
    public IncidentesService(RepositorioIncidentes repo) { this.repo = repo; }
    public List<Map<String, Object>> listar() { return repo.listar(); }
    public List<Map<String, Object>> resumo() { return repo.resumo(); }
    public List<Map<String, Object>> detalhes(String id) { return repo.detalhes(id); }
    public List<Map<String, Object>> acoes(String id) { return repo.acoes(id); }
    public List<Map<String, Object>> tipos() { return repo.tipos(); }
    public List<Map<String, Object>> severidades() { return repo.severidades(); }
    public List<Map<String, Object>> ativos(String id) { return repo.ativos(id); }
    public List<Map<String, Object>> historico(String id) { return repo.historico(id); }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANALISTA')")
    @Transactional(rollbackFor = Exception.class)
    public String abrir(String titulo, String descricao, int tipo, int severidade, String autor) {
        return repo.abrir(titulo.trim(), descricao.trim(), tipo, severidade, autor);
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANALISTA')")
    @Transactional(rollbackFor = Exception.class)
    public void iniciar(String id, String autor) { repo.iniciar(id, autor); }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANALISTA')")
    @Transactional(rollbackFor = Exception.class)
    public void encerrar(String id, String autor, String conclusao) { repo.encerrar(id, autor, conclusao.trim()); }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANALISTA')")
    @Transactional(rollbackFor = Exception.class)
    public void registrarAcao(String id, String autor, String descricao) {
        repo.registrarAcao(id, autor, descricao.trim());
    }
}
