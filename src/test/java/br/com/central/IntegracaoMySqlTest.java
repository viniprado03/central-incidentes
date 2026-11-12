package br.com.central;

import br.com.central.incidente.IncidentesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "central.demonstracao=true")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CENTRAL_INTEGRATION", matches = "true")
class IntegracaoMySqlTest {
    private static final String ANALISTA = "USR-00000000000000000000000000000002";
    @Autowired MockMvc mvc;
    @Autowired IncidentesService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transacao;

    @Test
    void loginComSenhaDoBanco() throws Exception {
        for (String email : new String[] {"admin@exemplo.invalid", "analista@exemplo.invalid", "visualizador@exemplo.invalid"}) {
            mvc.perform(formLogin().user(email).password("CentralDemo123!"))
                    .andExpect(authenticated().withUsername(email));
        }
        mvc.perform(formLogin().user("analista@exemplo.invalid").password("senha-incorreta"))
                .andExpect(unauthenticated());
    }

    @Test
    void acessosDeDemonstracaoTrocamUsuarioEPermissoes() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acesso de demonstração")));
        for (String perfil : new String[] {"administrador", "analista", "visualizador"}) {
            String email = perfil.equals("administrador") ? "admin@exemplo.invalid" : perfil + "@exemplo.invalid";
            mvc.perform(post("/demonstracao/entrar").with(csrf()).param("perfil", perfil))
                    .andExpect(status().is3xxRedirection()).andExpect(authenticated().withUsername(email));
        }
        var login = mvc.perform(post("/demonstracao/entrar").with(csrf()).param("perfil", "administrador"))
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trocar perfil")));
        mvc.perform(post("/demonstracao/entrar").session(session).with(csrf()).param("perfil", "visualizador"))
                .andExpect(authenticated().withUsername("visualizador@exemplo.invalid"));
        mvc.perform(get("/incidentes/novo").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/demonstracao/entrar").with(csrf()).param("perfil", "outro"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/demonstracao/entrar").param("perfil", "administrador"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VISUALIZADOR")
    void visualizadorNaoPodeAbrirIncidente() throws Exception {
        mvc.perform(post("/api/incidentes").with(csrf()).contentType("application/json")
                .content("{\"titulo\":\"Teste\",\"descricao\":\"Teste\",\"tipo\":1,\"severidade\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorConsultaPermissoes() throws Exception {
        mvc.perform(get("/permissoes")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Permissões dos perfis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Visualizador")));
    }

    @Test
    void outrosPerfisNaoAcessamPermissoes() throws Exception {
        for (String papel : new String[] {"ANALISTA", "VISUALIZADOR"}) {
            mvc.perform(get("/permissoes").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teste").roles(papel)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void analistaAutenticadoAbreIncidenteComIdentidadeDaSessao() throws Exception {
        var login = mvc.perform(formLogin().user("analista@exemplo.invalid").password("CentralDemo123!"))
                .andExpect(authenticated()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var resposta = mvc.perform(post("/api/incidentes").session(session).with(csrf())
                .contentType("application/json")
                .content("{\"titulo\":\"Teste HTTP\",\"descricao\":\"Teste\",\"tipo\":1,\"severidade\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(resposta).get("id").asText();
        assertThat(jdbc.queryForObject("SELECT registrante FROM vw_incidentes_detalhados WHERE id_incidente = ?", String.class, id))
                .isEqualTo("Analista de demonstração");
    }

    @Test
    void paginasEPermissoesDeVisualizador() throws Exception {
        var login = mvc.perform(formLogin().user("visualizador@exemplo.invalid").password("CentralDemo123!"))
                .andExpect(authenticated()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lista de incidentes")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Novo incidente"))));
        mvc.perform(get("/").session(session).param("status", "ENCERRADO").param("busca", "termo-inexistente"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum incidente encontrado")));
        mvc.perform(get("/informacoes").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Por severidade")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Por tipo")));
        mvc.perform(get("/incidentes/novo").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/incidentes").session(session).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void analistaVisualizaCadastroEDetalhes() throws Exception {
        var login = mvc.perform(formLogin().user("analista@exemplo.invalid").password("CentralDemo123!"))
                .andExpect(authenticated()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/incidentes/novo").session(session)).andExpect(status().isOk());
        String id = jdbc.queryForObject("SELECT id_incidente FROM vw_incidentes_detalhados LIMIT 1", String.class);
        mvc.perform(get("/incidentes/" + id).session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Histórico de status")));
    }

    @Test
    void formularioCompletaFluxoDeIncidente() throws Exception {
        var login = mvc.perform(formLogin().user("analista@exemplo.invalid").password("CentralDemo123!"))
                .andExpect(authenticated()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var criacao = mvc.perform(post("/incidentes").session(session).with(csrf())
                .param("titulo", "Fluxo pela tela").param("descricao", "Teste temporário")
                .param("tipo", "1").param("severidade", "2"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String url = criacao.getResponse().getRedirectedUrl();
        assertThat(url).startsWith("/incidentes/INC-");
        mvc.perform(post(url + "/iniciar").session(session).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(url + "/acoes").session(session).with(csrf()).param("descricao", "Investigação"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(url + "/encerrar").session(session).with(csrf()).param("descricao", "Concluído"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(url).session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Concluído")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ENCERRADO")));
    }

    @Test
    @WithMockUser(roles = "ANALISTA")
    void encerramentoParticipaDoRollbackDoSpring() {
        String id = service.abrir("Teste de integração", "Teste temporário", 1, 2, ANALISTA);
        service.iniciar(id, ANALISTA);
        transacao.executeWithoutResult(status -> {
            service.registrarAcao(id, ANALISTA, "Investigação iniciada");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acoes_resposta WHERE id_incidente = ?", Integer.class, id)).isEqualTo(1);
            status.setRollbackOnly();
        });
        transacao.executeWithoutResult(status -> {
            service.encerrar(id, ANALISTA, "Teste concluído");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acoes_resposta WHERE id_incidente = ?", Integer.class, id)).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT status FROM vw_incidentes_detalhados WHERE id_incidente = ?", String.class, id)).isEqualTo("EM ANÁLISE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acoes_resposta WHERE id_incidente = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historico_status WHERE id_incidente = ?", Integer.class, id)).isEqualTo(2);
    }
}
