package br.com.central.incidente;

import br.com.central.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidentes")
public class IncidentesController {
    private final IncidentesService service;
    public IncidentesController(IncidentesService service) { this.service = service; }
    public record Abertura(@NotBlank @Size(max=150) String titulo, @NotBlank String descricao,
                           @Min(1) @Max(255) int tipo, @Min(1) @Max(255) int severidade) {}
    public record Texto(@NotBlank String descricao) {}

    @GetMapping public List<Map<String,Object>> listar() { return service.listar(); }
    @GetMapping("/resumo") public List<Map<String,Object>> resumo() { return service.resumo(); }
    @GetMapping("/tipos") public List<Map<String,Object>> tipos() { return service.tipos(); }
    @GetMapping("/severidades") public List<Map<String,Object>> severidades() { return service.severidades(); }
    @GetMapping("/{id}/ativos") public List<Map<String,Object>> ativos(@PathVariable String id) { return service.ativos(id); }
    @GetMapping("/{id}") public List<Map<String,Object>> detalhes(@PathVariable String id) { return service.detalhes(id); }
    @GetMapping("/{id}/acoes") public List<Map<String,Object>> acoes(@PathVariable String id) { return service.acoes(id); }
    @GetMapping("/{id}/historico") public List<Map<String,Object>> historico(@PathVariable String id) { return service.historico(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> abrir(@Valid @RequestBody Abertura dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("id", service.abrir(dados.titulo(), dados.descricao(), dados.tipo(), dados.severidade(), usuario.getId()));
    }
    @PostMapping("/{id}/iniciar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciar(@PathVariable String id, @AuthenticationPrincipal UsuarioAutenticado usuario) { service.iniciar(id, usuario.getId()); }
    @PostMapping("/{id}/encerrar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrar(@PathVariable String id, @Valid @RequestBody Texto texto, @AuthenticationPrincipal UsuarioAutenticado usuario) { service.encerrar(id, usuario.getId(), texto.descricao()); }
    @PostMapping("/{id}/acoes") @ResponseStatus(HttpStatus.CREATED)
    public void acao(@PathVariable String id, @Valid @RequestBody Texto texto, @AuthenticationPrincipal UsuarioAutenticado usuario) { service.registrarAcao(id, usuario.getId(), texto.descricao()); }
}
