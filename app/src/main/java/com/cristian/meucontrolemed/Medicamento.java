package com.cristian.meucontrolemed;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Date;

public class Medicamento implements Serializable {
    @Exclude
    private String id;
    private String nome;
    private String dosagem;
    private Date horario;

    public Medicamento() {
        // Construtor vazio necessário para o Firestore
    }

    public Medicamento(String nome, String dosagem, Date horario) {
        this.nome = nome;
        this.dosagem = dosagem;
        this.horario = horario;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDosagem() { return dosagem; }
    public void setDosagem(String dosagem) { this.dosagem = dosagem; }

    public Date getHorario() { return horario; }
    public void setHorario(Date horario) { this.horario = horario; }
}

