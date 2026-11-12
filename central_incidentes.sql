CREATE DATABASE IF NOT EXISTS central_incidentes
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE central_incidentes;
CREATE TABLE grupos_usuarios (
  id_grupo TINYINT UNSIGNED PRIMARY KEY,
  nome VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;
CREATE TABLE usuarios (
  id_usuario CHAR(36) PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  senha_hash VARCHAR(255) NOT NULL,
  id_grupo TINYINT UNSIGNED NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_usuario_grupo FOREIGN KEY (id_grupo) REFERENCES grupos_usuarios(id_grupo),
  CONSTRAINT ck_usuario_ativo CHECK (ativo IN (0, 1))
) ENGINE=InnoDB;
CREATE TABLE tipos_incidente (
  id_tipo TINYINT UNSIGNED PRIMARY KEY,
  nome VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;
CREATE TABLE severidades (
  id_severidade TINYINT UNSIGNED PRIMARY KEY,
  nome VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;
CREATE TABLE status_incidente (
  id_status TINYINT UNSIGNED PRIMARY KEY,
  nome VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;
CREATE TABLE ativos (
  id_ativo CHAR(36) PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255)
) ENGINE=InnoDB;
CREATE TABLE incidentes (
  id_incidente CHAR(36) PRIMARY KEY,
  titulo VARCHAR(150) NOT NULL,
  descricao TEXT NOT NULL,
  id_tipo TINYINT UNSIGNED NOT NULL,
  id_severidade TINYINT UNSIGNED NOT NULL,
  id_status TINYINT UNSIGNED NOT NULL DEFAULT 1,
  id_registrante CHAR(36) NOT NULL,
  id_responsavel CHAR(36),
  id_autor_status CHAR(36) NOT NULL,
  data_abertura DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_encerramento DATETIME,
  CONSTRAINT fk_incidente_tipo FOREIGN KEY (id_tipo) REFERENCES tipos_incidente(id_tipo),
  CONSTRAINT fk_incidente_severidade FOREIGN KEY (id_severidade) REFERENCES severidades(id_severidade),
  CONSTRAINT fk_incidente_status FOREIGN KEY (id_status) REFERENCES status_incidente(id_status),
  CONSTRAINT fk_incidente_registrante FOREIGN KEY (id_registrante) REFERENCES usuarios(id_usuario),
  CONSTRAINT fk_incidente_responsavel FOREIGN KEY (id_responsavel) REFERENCES usuarios(id_usuario),
  CONSTRAINT fk_incidente_autor_status FOREIGN KEY (id_autor_status) REFERENCES usuarios(id_usuario),
  CONSTRAINT ck_incidente_titulo CHECK (CHAR_LENGTH(TRIM(titulo)) > 0),
  CONSTRAINT ck_incidente_descricao CHECK (CHAR_LENGTH(TRIM(descricao)) > 0),
  CONSTRAINT ck_incidente_encerramento CHECK (
    (id_status = 3 AND data_encerramento IS NOT NULL AND data_encerramento >= data_abertura)
    OR (id_status <> 3 AND data_encerramento IS NULL)
  ),
  CONSTRAINT ck_incidente_responsavel CHECK (id_status = 1 OR id_responsavel IS NOT NULL)
) ENGINE=InnoDB;
CREATE TABLE incidente_ativo (
  id_incidente CHAR(36) NOT NULL,
  id_ativo CHAR(36) NOT NULL,
  PRIMARY KEY (id_incidente, id_ativo),
  CONSTRAINT fk_ia_incidente FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente),
  CONSTRAINT fk_ia_ativo FOREIGN KEY (id_ativo) REFERENCES ativos(id_ativo)
) ENGINE=InnoDB;
CREATE TABLE acoes_resposta (
  id_acao CHAR(36) PRIMARY KEY,
  id_incidente CHAR(36) NOT NULL,
  id_autor CHAR(36) NOT NULL,
  descricao TEXT NOT NULL,
  conclusiva BOOLEAN NOT NULL DEFAULT FALSE,
  data_acao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_acao_incidente FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente),
  CONSTRAINT fk_acao_autor FOREIGN KEY (id_autor) REFERENCES usuarios(id_usuario),
  CONSTRAINT ck_acao_descricao CHECK (CHAR_LENGTH(TRIM(descricao)) > 0),
  CONSTRAINT ck_acao_conclusiva CHECK (conclusiva IN (0, 1))
) ENGINE=InnoDB;
CREATE TABLE historico_status (
  id_historico CHAR(36) PRIMARY KEY,
  id_incidente CHAR(36) NOT NULL,
  id_status_anterior TINYINT UNSIGNED,
  id_status_novo TINYINT UNSIGNED NOT NULL,
  id_autor CHAR(36) NOT NULL,
  data_alteracao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_historico_incidente FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente),
  CONSTRAINT fk_historico_anterior FOREIGN KEY (id_status_anterior) REFERENCES status_incidente(id_status),
  CONSTRAINT fk_historico_novo FOREIGN KEY (id_status_novo) REFERENCES status_incidente(id_status),
  CONSTRAINT fk_historico_autor FOREIGN KEY (id_autor) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;
USE central_incidentes;
START TRANSACTION;
INSERT INTO grupos_usuarios (id_grupo, nome) VALUES
  (1, 'ADMINISTRADOR'), (2, 'ANALISTA'), (3, 'VISUALIZADOR');
INSERT INTO tipos_incidente (id_tipo, nome) VALUES
  (1, 'Phishing'), (2, 'Malware'), (3, 'Acesso indevido'), (4, 'Outro');
INSERT INTO severidades (id_severidade, nome) VALUES
  (1, 'Baixa'), (2, 'Média'), (3, 'Alta'), (4, 'Crítica');
INSERT INTO status_incidente (id_status, nome) VALUES
  (1, 'ABERTO'), (2, 'EM ANÁLISE'), (3, 'ENCERRADO');
INSERT INTO usuarios (id_usuario, nome, email, senha_hash, id_grupo) VALUES
  ('USR-00000000000000000000000000000001', 'Administrador de demonstração', 'admin@exemplo.invalid', '$2y$10$Dvj3h7mGg1YC9SFuXFOmfevHTk4WjPwhr6Ov3XSZTUzA6dSRhSla6', 1),
  ('USR-00000000000000000000000000000002', 'Analista de demonstração', 'analista@exemplo.invalid', '$2y$10$Dvj3h7mGg1YC9SFuXFOmfevHTk4WjPwhr6Ov3XSZTUzA6dSRhSla6', 2),
  ('USR-00000000000000000000000000000003', 'Visualizador de demonstração', 'visualizador@exemplo.invalid', '$2y$10$Dvj3h7mGg1YC9SFuXFOmfevHTk4WjPwhr6Ov3XSZTUzA6dSRhSla6', 3);
INSERT INTO ativos (id_ativo, nome, descricao) VALUES
  ('ATI-00000000000000000000000000000001', 'Estação de trabalho', 'Ativo fictício para demonstração');
COMMIT;
CREATE USER 'central_provisionador'@'localhost' IDENTIFIED BY 'prov123';
CREATE USER 'central_app'@'localhost' IDENTIFIED BY 'app123';
CREATE USER 'central_leitura'@'localhost' IDENTIFIED BY 'leit123';
GRANT SELECT, INSERT, UPDATE, CREATE ROUTINE, ALTER ROUTINE, EXECUTE,
      CREATE VIEW, SHOW VIEW, TRIGGER, INDEX
ON central_incidentes.* TO 'central_provisionador'@'localhost';
USE central_incidentes;
DELIMITER $$
CREATE DEFINER = 'central_provisionador'@'localhost' FUNCTION fn_gerar_id_usuario() RETURNS CHAR(36)
NOT DETERMINISTIC NO SQL
BEGIN
    RETURN CONCAT('USR-', REPLACE(UUID(), '-', ''));
END$$
CREATE DEFINER = 'central_provisionador'@'localhost' FUNCTION fn_gerar_id_incidente() RETURNS CHAR(36)
NOT DETERMINISTIC NO SQL
BEGIN
    RETURN CONCAT('INC-', REPLACE(UUID(), '-', ''));
END$$
CREATE DEFINER = 'central_provisionador'@'localhost' FUNCTION fn_gerar_id_operacional(prefixo CHAR(3)) RETURNS CHAR(36)
NOT DETERMINISTIC NO SQL
BEGIN
    IF prefixo IS NULL OR UPPER(prefixo) NOT IN ('ATI', 'ACA', 'HIS') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Prefixo operacional inválido';
    END IF;
    RETURN CONCAT(UPPER(prefixo), '-', REPLACE(UUID(), '-', ''));
END$$
DELIMITER ;
USE central_incidentes;
DELIMITER $$
CREATE DEFINER = 'central_provisionador'@'localhost' TRIGGER trg_incidente_criado AFTER INSERT ON incidentes
FOR EACH ROW
BEGIN
    INSERT INTO historico_status (id_historico, id_incidente, id_status_anterior, id_status_novo, id_autor)
    VALUES (fn_gerar_id_operacional('HIS'), NEW.id_incidente, NULL, NEW.id_status, NEW.id_autor_status);
END$$
CREATE DEFINER = 'central_provisionador'@'localhost' TRIGGER trg_incidente_status AFTER UPDATE ON incidentes
FOR EACH ROW
BEGIN
    IF OLD.id_status <> NEW.id_status THEN
        INSERT INTO historico_status (id_historico, id_incidente, id_status_anterior, id_status_novo, id_autor)
        VALUES (fn_gerar_id_operacional('HIS'), NEW.id_incidente, OLD.id_status, NEW.id_status, NEW.id_autor_status);
    END IF;
END$$
DELIMITER ;
USE central_incidentes;
DELIMITER $$
CREATE DEFINER = 'central_provisionador'@'localhost' PROCEDURE sp_abrir_incidente(
    IN p_titulo VARCHAR(150), IN p_descricao TEXT,
    IN p_tipo TINYINT UNSIGNED, IN p_severidade TINYINT UNSIGNED,
    IN p_autor CHAR(36), OUT p_id CHAR(36)
)
SQL SECURITY DEFINER
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_autor AND ativo = 1 AND id_grupo IN (1, 2)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuário sem permissão para abrir incidente';
    END IF;
    SET p_id = fn_gerar_id_incidente();
    INSERT INTO incidentes (id_incidente, titulo, descricao, id_tipo, id_severidade, id_registrante, id_autor_status)
    VALUES (p_id, p_titulo, p_descricao, p_tipo, p_severidade, p_autor, p_autor);
END$$
CREATE DEFINER = 'central_provisionador'@'localhost' PROCEDURE sp_iniciar_analise(IN p_incidente CHAR(36), IN p_analista CHAR(36))
SQL SECURITY DEFINER
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_analista AND ativo = 1 AND id_grupo IN (1, 2)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuário sem permissão para analisar';
    END IF;
    UPDATE incidentes SET id_status = 2, id_responsavel = p_analista, id_autor_status = p_analista
    WHERE id_incidente = p_incidente AND id_status = 1;
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Incidente inexistente ou não está aberto';
    END IF;
END$$
CREATE DEFINER = 'central_provisionador'@'localhost' PROCEDURE sp_encerrar_incidente(IN p_incidente CHAR(36), IN p_autor CHAR(36), IN p_conclusao TEXT)
SQL SECURITY DEFINER
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_autor AND ativo = 1 AND id_grupo IN (1, 2)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuário sem permissão para encerrar';
    END IF;
    IF p_conclusao IS NULL OR CHAR_LENGTH(TRIM(p_conclusao)) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Informe a ação conclusiva';
    END IF;
    UPDATE incidentes SET id_status = 3, data_encerramento = CURRENT_TIMESTAMP, id_autor_status = p_autor
    WHERE id_incidente = p_incidente AND id_status = 2;
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Incidente inexistente ou não está em análise';
    END IF;
    INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao, conclusiva)
    VALUES (fn_gerar_id_operacional('ACA'), p_incidente, p_autor, p_conclusao, 1);
END$$
DELIMITER ;
USE central_incidentes;
CREATE DEFINER = 'central_provisionador'@'localhost' SQL SECURITY DEFINER VIEW vw_resumo_incidentes AS
SELECT s.id_status, s.nome AS status, sev.id_severidade, sev.nome AS severidade,
       COUNT(*) AS total
FROM incidentes i
JOIN status_incidente s ON s.id_status = i.id_status
JOIN severidades sev ON sev.id_severidade = i.id_severidade
GROUP BY s.id_status, s.nome, sev.id_severidade, sev.nome;
CREATE DEFINER = 'central_provisionador'@'localhost' SQL SECURITY DEFINER VIEW vw_incidentes_detalhados AS
SELECT i.id_incidente, i.titulo, i.descricao, t.nome AS tipo, sev.nome AS severidade,
       s.nome AS status, registrante.nome AS registrante, responsavel.nome AS responsavel,
       i.data_abertura, i.data_encerramento
FROM incidentes i
JOIN tipos_incidente t ON t.id_tipo = i.id_tipo
JOIN severidades sev ON sev.id_severidade = i.id_severidade
JOIN status_incidente s ON s.id_status = i.id_status
JOIN usuarios registrante ON registrante.id_usuario = i.id_registrante
LEFT JOIN usuarios responsavel ON responsavel.id_usuario = i.id_responsavel;
CREATE INDEX idx_incidentes_status_data ON incidentes (id_status, data_abertura);
CREATE INDEX idx_incidentes_severidade_data ON incidentes (id_severidade, data_abertura);
CREATE ROLE 'papel_central_app'@'localhost';
CREATE ROLE 'papel_central_leitura'@'localhost';
GRANT SELECT ON central_incidentes.vw_resumo_incidentes TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.vw_incidentes_detalhados TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.usuarios TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.grupos_usuarios TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.tipos_incidente TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.severidades TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.status_incidente TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.ativos TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.incidente_ativo TO 'papel_central_app'@'localhost';
GRANT SELECT, INSERT ON central_incidentes.acoes_resposta TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.historico_status TO 'papel_central_app'@'localhost';
GRANT EXECUTE ON PROCEDURE central_incidentes.sp_abrir_incidente TO 'papel_central_app'@'localhost';
GRANT EXECUTE ON PROCEDURE central_incidentes.sp_iniciar_analise TO 'papel_central_app'@'localhost';
GRANT EXECUTE ON PROCEDURE central_incidentes.sp_encerrar_incidente TO 'papel_central_app'@'localhost';
GRANT EXECUTE ON FUNCTION central_incidentes.fn_gerar_id_operacional TO 'papel_central_app'@'localhost';
GRANT SELECT ON central_incidentes.vw_resumo_incidentes TO 'papel_central_leitura'@'localhost';
GRANT SELECT ON central_incidentes.vw_incidentes_detalhados TO 'papel_central_leitura'@'localhost';
GRANT 'papel_central_app'@'localhost' TO 'central_app'@'localhost';
GRANT 'papel_central_leitura'@'localhost' TO 'central_leitura'@'localhost';
SET DEFAULT ROLE 'papel_central_app'@'localhost' TO 'central_app'@'localhost';
SET DEFAULT ROLE 'papel_central_leitura'@'localhost' TO 'central_leitura'@'localhost';
SHOW GRANTS FOR 'central_app'@'localhost';
SHOW GRANTS FOR 'central_leitura'@'localhost';
SHOW GRANTS FOR 'papel_central_app'@'localhost';
SHOW GRANTS FOR 'papel_central_leitura'@'localhost';
USE central_incidentes;
SET @admin = 'USR-00000000000000000000000000000001';
SET @analista = 'USR-00000000000000000000000000000002';
START TRANSACTION;
CALL sp_abrir_incidente(
    'E-mail suspeito solicitando troca de senha',
    'Uma mensagem pede que o usuário acesse um endereço desconhecido para atualizar a senha.',
    1, 2, @analista, @incidente_aberto_1
);
CALL sp_abrir_incidente(
    'Tentativas de acesso a conta desativada',
    'Foram identificadas tentativas de autenticação em uma conta que deveria estar desativada.',
    3, 3, @admin, @incidente_aberto_2
);
CALL sp_abrir_incidente(
    'Arquivo desconhecido recebido por e-mail',
    'Um colaborador recebeu um arquivo inesperado e informou a equipe antes de abri-lo.',
    4, 1, @analista, @incidente_aberto_3
);
CALL sp_abrir_incidente(
    'Possível malware em estação de trabalho',
    'A ferramenta de proteção da estação indicou um arquivo potencialmente malicioso.',
    2, 3, @analista, @incidente_analise_1
);
CALL sp_iniciar_analise(@incidente_analise_1, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @incidente_analise_1, @analista,
        'Estação isolada da rede para análise do arquivo.');
CALL sp_abrir_incidente(
    'Acesso não reconhecido a uma conta',
    'O usuário relatou uma sessão que não reconhece em sua conta corporativa.',
    3, 4, @admin, @incidente_analise_2
);
CALL sp_iniciar_analise(@incidente_analise_2, @admin);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @incidente_analise_2, @admin,
        'Sessões ativas encerradas e registros de acesso separados para análise.');
CALL sp_abrir_incidente(
    'Página falsa de acesso ao correio eletrônico',
    'Foi identificado um endereço que imita a página de entrada do correio da organização.',
    1, 3, @analista, @incidente_analise_3
);
CALL sp_iniciar_analise(@incidente_analise_3, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @incidente_analise_3, @analista,
        'Endereço reportado para bloqueio e mensagem de orientação preparada.');
CALL sp_abrir_incidente(
    'Mensagem de phishing enviada à equipe',
    'Uma mensagem se passou pelo suporte e solicitou informações de acesso.',
    1, 2, @analista, @incidente_encerrado_1
);
CALL sp_iniciar_analise(@incidente_encerrado_1, @analista);
CALL sp_encerrar_incidente(@incidente_encerrado_1, @analista,
    'Mensagem removida das caixas afetadas e usuários orientados a não acessar o link.');
CALL sp_abrir_incidente(
    'Programa indesejado encontrado em computador',
    'Uma verificação identificou um programa não autorizado em uma estação.',
    2, 2, @admin, @incidente_encerrado_2
);
CALL sp_iniciar_analise(@incidente_encerrado_2, @admin);
CALL sp_encerrar_incidente(@incidente_encerrado_2, @admin,
    'Programa removido e nova verificação concluída sem apontamentos.');
CALL sp_abrir_incidente(
    'Compartilhamento indevido de documento',
    'Um documento interno foi compartilhado com um destinatário externo por engano.',
    4, 3, @analista, @incidente_encerrado_3
);
CALL sp_iniciar_analise(@incidente_encerrado_3, @analista);
CALL sp_encerrar_incidente(@incidente_encerrado_3, @analista,
    'Acesso ao documento revogado e responsável orientado sobre o compartilhamento.');
CALL sp_abrir_incidente('Acesso suspeito após várias senhas incorretas',
    'Foram registradas tentativas malsucedidas antes de um acesso à conta.', 3, 3, @analista, @extra_01);
CALL sp_abrir_incidente('Mensagem falsa sobre cobrança pendente',
    'Uma mensagem pede pagamento por um endereço que não pertence à organização.', 1, 2, @analista, @extra_02);
CALL sp_abrir_incidente('Software não autorizado em equipamento',
    'Foi identificada a instalação de um programa sem autorização prévia.', 2, 2, @admin, @extra_03);
CALL sp_abrir_incidente('Envio de documento ao destinatário incorreto',
    'Um documento interno foi enviado para um destinatário diferente do pretendido.', 4, 2, @analista, @extra_04);
CALL sp_abrir_incidente('Pedido suspeito de confirmação de conta',
    'Uma mensagem solicita confirmação de credenciais por meio de formulário desconhecido.', 1, 1, @analista, @extra_05);
CALL sp_abrir_incidente('Alerta de execução de arquivo desconhecido',
    'A ferramenta de proteção apontou a execução de um arquivo desconhecido.', 2, 4, @admin, @extra_06);
CALL sp_abrir_incidente('Acesso a pasta fora do horário habitual',
    'Foi registrada uma consulta a uma pasta fora do horário habitual de trabalho.', 3, 2, @analista, @extra_07);
CALL sp_abrir_incidente('Dispositivo removível não identificado',
    'Um dispositivo removível desconhecido foi encontrado próximo a uma estação de trabalho.', 4, 1, @analista, @extra_08);
CALL sp_abrir_incidente('Solicitação falsa em nome da equipe de suporte',
    'Uma pessoa recebeu instruções para informar dados de acesso em um formulário externo.', 1, 3, @admin, @extra_09);
CALL sp_abrir_incidente('Acesso inesperado ao painel administrativo',
    'Uma tentativa de acesso ao painel administrativo precisa ser investigada.', 3, 4, @analista, @extra_10);
CALL sp_iniciar_analise(@extra_10, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_10, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Arquivo compactado suspeito recebido',
    'Um anexo compactado foi recebido de um remetente que o destinatário não reconhece.', 2, 3, @analista, @extra_11);
CALL sp_iniciar_analise(@extra_11, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_11, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Link de compartilhamento aberto ao público',
    'Um link de acesso a documentos foi identificado com visibilidade maior do que a prevista.', 4, 3, @admin, @extra_12);
CALL sp_iniciar_analise(@extra_12, @admin);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_12, @admin,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Tentativa de roubo de credenciais por mensagem',
    'Uma mensagem orientou usuários a fornecer credenciais em uma página externa.', 1, 3, @analista, @extra_13);
CALL sp_iniciar_analise(@extra_13, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_13, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Alerta de atividade maliciosa em estação',
    'Uma estação apresentou um alerta de comportamento associado a software malicioso.', 2, 4, @analista, @extra_14);
CALL sp_iniciar_analise(@extra_14, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_14, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Conta acessada em local não reconhecido',
    'O titular de uma conta não reconhece a origem de um acesso recente.', 3, 3, @admin, @extra_15);
CALL sp_iniciar_analise(@extra_15, @admin);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_15, @admin,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Perda de equipamento corporativo',
    'Um equipamento de trabalho foi informado como perdido pelo responsável.', 4, 3, @analista, @extra_16);
CALL sp_iniciar_analise(@extra_16, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_16, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Anexo suspeito em mensagem interna',
    'Uma conta interna encaminhou um anexo inesperado para vários destinatários.', 1, 2, @analista, @extra_17);
CALL sp_iniciar_analise(@extra_17, @analista);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_17, @analista,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Aplicativo desconhecido com acesso à conta',
    'Foi encontrado um aplicativo que o titular não reconhece entre os acessos da conta.', 3, 2, @admin, @extra_18);
CALL sp_iniciar_analise(@extra_18, @admin);
INSERT INTO acoes_resposta (id_acao, id_incidente, id_autor, descricao)
VALUES (fn_gerar_id_operacional('ACA'), @extra_18, @admin,
    'Informações iniciais registradas para investigação.');
CALL sp_abrir_incidente('Arquivo malicioso detectado no download',
    'A ferramenta de proteção sinalizou um arquivo baixado por um colaborador.', 2, 3, @analista, @extra_19);
CALL sp_iniciar_analise(@extra_19, @analista);
CALL sp_encerrar_incidente(@extra_19, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Cópia de documento para pasta incorreta',
    'Um arquivo interno foi copiado para uma pasta com permissões inadequadas.', 4, 1, @analista, @extra_20);
CALL sp_iniciar_analise(@extra_20, @analista);
CALL sp_encerrar_incidente(@extra_20, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Mensagem falsa solicitando dados bancários',
    'Uma mensagem fingiu ser da administração e pediu informações financeiras de colaboradores.', 1, 4, @admin, @extra_21);
CALL sp_iniciar_analise(@extra_21, @admin);
CALL sp_encerrar_incidente(@extra_21, @admin,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Tentativa de acesso a conta privilegiada',
    'Foram identificadas tentativas de autenticação em uma conta com privilégios elevados.', 3, 4, @analista, @extra_22);
CALL sp_iniciar_analise(@extra_22, @analista);
CALL sp_encerrar_incidente(@extra_22, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Complemento de navegador não autorizado',
    'Um complemento de navegador foi instalado sem passar pela avaliação da equipe.', 2, 1, @analista, @extra_23);
CALL sp_iniciar_analise(@extra_23, @analista);
CALL sp_encerrar_incidente(@extra_23, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Documento interno compartilhado externamente',
    'Um documento foi compartilhado fora da organização sem a confirmação do responsável.', 4, 2, @admin, @extra_24);
CALL sp_iniciar_analise(@extra_24, @admin);
CALL sp_encerrar_incidente(@extra_24, @admin,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Falso aviso de expiração de conta',
    'Uma mensagem alegou que a conta seria encerrada e solicitou acesso a uma página desconhecida.', 1, 2, @analista, @extra_25);
CALL sp_iniciar_analise(@extra_25, @analista);
CALL sp_encerrar_incidente(@extra_25, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Programa suspeito em pasta compartilhada',
    'Um arquivo executável desconhecido foi identificado em uma pasta compartilhada.', 2, 3, @analista, @extra_26);
CALL sp_iniciar_analise(@extra_26, @analista);
CALL sp_encerrar_incidente(@extra_26, @analista,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
CALL sp_abrir_incidente('Permissão de acesso concedida por engano',
    'Um usuário recebeu acesso a uma pasta fora de suas atribuições.', 3, 2, @admin, @extra_27);
CALL sp_iniciar_analise(@extra_27, @admin);
CALL sp_encerrar_incidente(@extra_27, @admin,
    'Ocorrência analisada, acesso necessário revisto e responsáveis orientados.');
COMMIT;
SELECT id_incidente, titulo, tipo, severidade, status, responsavel
FROM vw_incidentes_detalhados
WHERE id_incidente IN (
    @incidente_aberto_1, @incidente_aberto_2, @incidente_aberto_3,
    @incidente_analise_1, @incidente_analise_2, @incidente_analise_3,
    @incidente_encerrado_1, @incidente_encerrado_2, @incidente_encerrado_3,
    @extra_01, @extra_02, @extra_03, @extra_04, @extra_05, @extra_06, @extra_07, @extra_08, @extra_09,
    @extra_10, @extra_11, @extra_12, @extra_13, @extra_14, @extra_15, @extra_16, @extra_17, @extra_18,
    @extra_19, @extra_20, @extra_21, @extra_22, @extra_23, @extra_24, @extra_25, @extra_26, @extra_27
)
ORDER BY status, titulo;
