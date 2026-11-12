package br.com.central.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class InicioController {
    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/permissoes")
    public String permissoes(Model model) {
        model.addAttribute("aba", "permissoes");
        return "permissoes";
    }
}
