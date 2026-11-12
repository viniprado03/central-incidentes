package br.com.central.incidente;

import br.com.central.seguranca.UsuarioAutenticado;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
public class PaginasController {
    private final IncidentesService service;
    public PaginasController(IncidentesService service) { this.service = service; }

    @GetMapping("/")
    public String painel(@RequestParam(defaultValue = "") String busca,
                         @RequestParam(defaultValue = "") String status, Model model) {
        var lista = service.listar();
        String termo = busca.trim().toLowerCase(java.util.Locale.ROOT);
        String filtro = java.util.Set.of("ABERTO", "EM ANÁLISE", "ENCERRADO").contains(status) ? status : "";
        var filtrados = lista.stream().filter(i -> filtro.isEmpty() || filtro.equals(i.get("status")))
                .filter(i -> termo.isEmpty() || java.util.stream.Stream.of("titulo", "id_incidente", "tipo", "responsavel")
                        .map(i::get).filter(java.util.Objects::nonNull)
                        .anyMatch(v -> v.toString().toLowerCase(java.util.Locale.ROOT).contains(termo)))
                .toList();
        model.addAttribute("incidentes", filtrados);
        model.addAttribute("busca", busca);
        model.addAttribute("statusSelecionado", filtro);
        model.addAttribute("quantidade", filtrados.size());
        var resumo = service.resumo();
        model.addAttribute("total", resumo.stream().mapToLong(i -> ((Number) i.get("total")).longValue()).sum());
        model.addAttribute("abertos", totalPorStatus(resumo, "ABERTO"));
        model.addAttribute("analise", totalPorStatus(resumo, "EM ANÁLISE"));
        model.addAttribute("encerrados", totalPorStatus(resumo, "ENCERRADO"));
        return "painel";
    }

    @GetMapping("/informacoes")
    public String informacoes(Model model) {
        var incidentes = service.listar();
        var resumo = service.resumo();
        model.addAttribute("aba", "informacoes");
        model.addAttribute("total", incidentes.size());
        model.addAttribute("abertos", totalPorStatus(resumo, "ABERTO"));
        model.addAttribute("analise", totalPorStatus(resumo, "EM ANÁLISE"));
        model.addAttribute("encerrados", totalPorStatus(resumo, "ENCERRADO"));
        model.addAttribute("porTipo", contarPorCampo(incidentes, "tipo"));
        model.addAttribute("porSeveridade", contarPorCampo(incidentes, "severidade"));
        return "informacoes";
    }

    private java.util.Map<String, Long> contarPorCampo(java.util.List<Map<String,Object>> incidentes, String campo) {
        return incidentes.stream().collect(java.util.stream.Collectors.groupingBy(
                i -> i.get(campo).toString(), java.util.TreeMap::new, java.util.stream.Collectors.counting()));
    }

    private long totalPorStatus(java.util.List<Map<String,Object>> resumo, String status) {
        return resumo.stream().filter(i -> status.equals(i.get("status")))
                .mapToLong(i -> ((Number) i.get("total")).longValue()).sum();
    }

    @GetMapping("/incidentes/novo")
    public String novo(Model model) {
        model.addAttribute("tipos", service.tipos());
        model.addAttribute("severidades", service.severidades());
        return "novo";
    }

    @PostMapping("/incidentes")
    public String criar(@RequestParam String titulo, @RequestParam String descricao,
                        @RequestParam int tipo, @RequestParam int severidade,
                        @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes flash) {
        if (titulo.isBlank() || titulo.trim().length() > 150 || descricao.isBlank()) {
            flash.addFlashAttribute("erro", "Preencha título e descrição. O título deve ter até 150 caracteres.");
            return "redirect:/incidentes/novo";
        }
        try {
            String id = service.abrir(titulo, descricao, tipo, severidade, usuario.getId());
            flash.addFlashAttribute("sucesso", "Incidente registrado.");
            return "redirect:/incidentes/" + id;
        } catch (DataAccessException e) {
            flash.addFlashAttribute("erro", "Não foi possível registrar o incidente. Confira os dados e tente novamente.");
            return "redirect:/incidentes/novo";
        }
    }

    @GetMapping("/incidentes/{id}")
    public String detalhe(@PathVariable String id, Model model) {
        Map<String,Object> incidente = service.detalhes(id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("incidente", incidente);
        model.addAttribute("acoes", service.acoes(id));
        model.addAttribute("historico", service.historico(id));
        model.addAttribute("ativos", service.ativos(id));
        return "detalhe";
    }

    @PostMapping("/incidentes/{id}/iniciar")
    public String iniciar(@PathVariable String id, @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes flash) {
        try { service.iniciar(id, usuario.getId()); flash.addFlashAttribute("sucesso", "Análise iniciada."); }
        catch (DataAccessException e) { flash.addFlashAttribute("erro", "Não foi possível iniciar a análise. Confira o status do incidente."); }
        return "redirect:/incidentes/" + id;
    }
    @PostMapping("/incidentes/{id}/acoes")
    public String acao(@PathVariable String id, @RequestParam String descricao, @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes flash) {
        if (descricao.isBlank()) { flash.addFlashAttribute("erro", "Descreva a ação antes de registrar."); return "redirect:/incidentes/" + id; }
        try { service.registrarAcao(id, usuario.getId(), descricao); flash.addFlashAttribute("sucesso", "Ação registrada."); }
        catch (DataAccessException | IllegalStateException e) { flash.addFlashAttribute("erro", "Não foi possível registrar a ação. Confira se o incidente está em análise."); }
        return "redirect:/incidentes/" + id;
    }
    @PostMapping("/incidentes/{id}/encerrar")
    public String encerrar(@PathVariable String id, @RequestParam String descricao, @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes flash) {
        if (descricao.isBlank()) { flash.addFlashAttribute("erro", "Descreva a ação conclusiva para encerrar."); return "redirect:/incidentes/" + id; }
        try { service.encerrar(id, usuario.getId(), descricao); flash.addFlashAttribute("sucesso", "Incidente encerrado."); }
        catch (DataAccessException e) { flash.addFlashAttribute("erro", "Não foi possível encerrar. Confira o status e tente novamente."); }
        return "redirect:/incidentes/" + id;
    }
}
