package com.example.chat_hub.controlador;

import com.example.chat_hub.servico.ServicoUtilizador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A classe ControladorRegisto gere as operações relacionadas ao registo de
 * utilizadores.
 */
@Controller
public class ControladorRegisto {

    @Autowired
    private ServicoUtilizador servicoUtilizador;

    /**
     * Exibe a página de registro.
     *
     * @return o nome do template da página de registro.
     */
    @GetMapping("/registro")
    public String mostrarPaginaDeRegistro() {
        return "registro";
    }

    /**
     * Realiza o registro de um novo utilizador.
     *
     * @param nomeUtilizador     o nome do utilizador.
     * @param senha              a senha do utilizador.
     * @param redirectAttributes atributos de redirecionamento para exibir
     *                           mensagens.
     * @return o redirecionamento para a página inicial.
     */
    @PostMapping("/registrar")
    public String registo(@RequestParam String nomeUtilizador, @RequestParam String senha,
            RedirectAttributes redirectAttributes) {
        servicoUtilizador.registo(nomeUtilizador, senha);
        redirectAttributes.addFlashAttribute("mensagem",
                "Registro bem-sucedido! Bem-vindo ao ChatHub, " + nomeUtilizador + "!");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");
        return "redirect:/login";
    }

    /**
     * Altera o registro de um utilizador existente.
     *
     * @param nomeUtilizador o nome do utilizador.
     * @param novaSenha      a nova senha do utilizador.
     * @return o redirecionamento para a página inicial.
     */
    @PostMapping("/alterar-registro")
    public String alterarRegistro(@RequestParam String nomeUtilizador, @RequestParam String novaSenha,
            RedirectAttributes redirectAttributes) {
        servicoUtilizador.alterarSenha(nomeUtilizador, novaSenha);
        redirectAttributes.addFlashAttribute("mensagem",
                "Senha alterada com sucesso para o utilizador " + nomeUtilizador + "!");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");
        return "redirect:/";
    }

    /**
     * Apaga o registro de um utilizador existente.
     *
     * @param nomeUtilizador o nome do utilizador a ser apagado.
     * @return o redirecionamento para a página inicial.
     */
    @PostMapping("/apagar-registro")
    public String apagarRegistro(@RequestParam String nomeUtilizador, RedirectAttributes redirectAttributes) {
        servicoUtilizador.apagarUsuario(nomeUtilizador);
        redirectAttributes.addFlashAttribute("mensagem", "Utilizador " + nomeUtilizador + " apagado com sucesso!");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");
        return "redirect:/";
    }

}
