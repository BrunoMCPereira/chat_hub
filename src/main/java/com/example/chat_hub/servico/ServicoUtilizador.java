package com.example.chat_hub.servico;

import com.example.chat_hub.modelo.Utilizador;
import com.example.chat_hub.zookeeper.GerenciadorZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoUtilizador {

    @Autowired
    private GerenciadorZooKeeper gerenciadorZooKeeper;

    // Lista nomes dos utilizadores por estado
    public List<String> listarNomesUtilizadoresPorStatus(String estado) {
        return gerenciadorZooKeeper.listarUtilizadoresPorStatusZook(estado);
    }

    // Registra um novo utilizador
    public void registo(String nomeUtilizador, String senha) {
        if (!existeUsuario(nomeUtilizador)) {
            Utilizador utilizador = new Utilizador();
            utilizador.setNomeUtilizador(nomeUtilizador);
            utilizador.setSenha(hashSenha(senha));
            utilizador.setStatus("offline");
            gerenciadorZooKeeper.registrarUsuario(utilizador);
            gerenciadorZooKeeper.registrarNovoUsuario(nomeUtilizador);
        }
    }

    // Altera o estado de um utilizador
    public void alterarEstado(String nomeUtilizador, String estado) {
        gerenciadorZooKeeper.alterarEstadoUsuario(nomeUtilizador, estado);
        gerenciadorZooKeeper.atualizarEstadoUtilizador(nomeUtilizador, estado);
    }

    // Valida um utilizador
    public boolean validarUsuario(String nomeUtilizador, String senha) {
        return gerenciadorZooKeeper.validarUsuario(nomeUtilizador, senha);
    }

    // Altera a senha de um utilizador
    public void alterarSenha(String nomeUtilizador, String novaSenha) {
        List<Utilizador> usuarios = gerenciadorZooKeeper.listarUtilizadoresPorNome(nomeUtilizador);
        for (Utilizador usuario : usuarios) {
            if (usuario != null) {
                usuario.setSenha(hashSenha(novaSenha));
                gerenciadorZooKeeper.atualizarUsuario(usuario);
                break;
            }
        }
    }

    // Apaga um utilizador
    public void apagarUsuario(String nomeUtilizador) {
        List<Utilizador> usuarios = gerenciadorZooKeeper.listarUtilizadoresPorNome(nomeUtilizador);
        for (Utilizador usuario : usuarios) {
            if (usuario != null) {
                gerenciadorZooKeeper.apagarUtilizador(usuario.getNomeUtilizador());
                gerenciadorZooKeeper.apagarUsuario(usuario);
                break;
            }
        }
    }

    // Verifica se um utilizador já existe
    public boolean existeUsuario(String nomeUtilizador) {
        return gerenciadorZooKeeper.existeUsuario(nomeUtilizador);
    }

    // Hash da senha
    private String hashSenha(String senha) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(senha);
    }

}
