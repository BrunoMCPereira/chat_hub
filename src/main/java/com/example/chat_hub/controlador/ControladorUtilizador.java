package com.example.chat_hub.controlador;

import com.example.chat_hub.servico.ServicoUtilizador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorUtilizador {

    @Autowired
    private ServicoUtilizador servicoUtilizador;

    @PostMapping("/estado_ligacao")
    public void alterarEstado(@RequestParam String nomeUtilizador,
            @RequestParam String estado) {
        servicoUtilizador.alterarEstado(nomeUtilizador, estado);

    }

}
