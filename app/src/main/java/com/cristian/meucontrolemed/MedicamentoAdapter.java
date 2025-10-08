package com.cristian.meucontrolemed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder> {

    private final List<Medicamento> listaMedicamentos;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Medicamento medicamento);
    }

    public MedicamentoAdapter(List<Medicamento> listaMedicamentos, OnItemClickListener listener) {
        this.listaMedicamentos = listaMedicamentos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento, parent, false);
        return new MedicamentoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicamentoViewHolder holder, int position) {
        Medicamento medicamentoAtual = listaMedicamentos.get(position);
        holder.bind(medicamentoAtual, listener);
    }

    @Override
    public int getItemCount() {
        return listaMedicamentos.size();
    }

    static class MedicamentoViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewNome;
        private final TextView textViewDosagem;
        private final TextView textViewHorario;

        public MedicamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNome = itemView.findViewById(R.id.textViewNomeMedicamento);
            textViewDosagem = itemView.findViewById(R.id.textViewDosagem);
            textViewHorario = itemView.findViewById(R.id.textViewHorario);
        }

        public void bind(final Medicamento medicamento, final OnItemClickListener listener) {
            textViewNome.setText(medicamento.getNome());
            textViewDosagem.setText(medicamento.getDosagem());

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            textViewHorario.setText(sdf.format(medicamento.getHorario()));

            itemView.setOnClickListener(v -> listener.onItemClick(medicamento));
        }
    }
}

