package com.cristian.meucontrolemed;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.io.Serializable;

public class AdicionarMedicamentoActivity extends AppCompatActivity {

    private static final String TAG = "MeuControleMedDebug";

    private EditText editTextNomeMedicamento, editTextDosagem;
    private TimePicker timePicker;
    private Button buttonSalvar, buttonExcluir, buttonVoltar;

    private FirebaseFirestore db;
    private Medicamento medicamentoAtual;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_medicamento);
        Log.d(TAG, "Tela AdicionarMedicamentoActivity iniciada (onCreate).");

        db = FirebaseFirestore.getInstance();
        editTextNomeMedicamento = findViewById(R.id.editTextNomeMedicamento);
        editTextDosagem = findViewById(R.id.editTextDosagem);
        timePicker = findViewById(R.id.timePicker);
        buttonSalvar = findViewById(R.id.buttonSalvar);
        buttonExcluir = findViewById(R.id.buttonExcluir);
        buttonVoltar = findViewById(R.id.buttonVoltar);

        timePicker.setIs24HourView(true);

        if (getIntent().hasExtra("medicamento_para_editar")) {
            medicamentoAtual = (Medicamento) getIntent().getSerializableExtra("medicamento_para_editar");
            if (medicamentoAtual != null) {
                isEditMode = true;
                preencherDados();
            }
        }

        buttonSalvar.setOnClickListener(view -> {
            Log.d(TAG, "Botão SALVAR foi clicado.");
            salvarMedicamento();
        });

        buttonExcluir.setOnClickListener(view -> {
            Log.d(TAG, "Botão EXCLUIR foi clicado.");
            excluirMedicamento();
        });

        buttonVoltar.setOnClickListener(view -> {
            Log.d(TAG, "Botão VOLTAR foi clicado.");
            finish();
        });

        Log.d(TAG, "Listeners dos botões foram configurados.");
    }

    private void preencherDados() {
        setTitle("Editar Medicamento");
        editTextNomeMedicamento.setText(medicamentoAtual.getNome());
        editTextDosagem.setText(medicamentoAtual.getDosagem());

        Calendar cal = Calendar.getInstance();
        cal.setTime(medicamentoAtual.getHorario());

        // Corrigido para usar a API não depreciada
        timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(cal.get(Calendar.MINUTE));

        buttonSalvar.setText("Alterar");
        buttonExcluir.setVisibility(View.VISIBLE);
    }

    private void salvarMedicamento() {
        Log.d(TAG, "Método salvarMedicamento() chamado.");
        String nome = editTextNomeMedicamento.getText().toString().trim();
        String dosagem = editTextDosagem.getText().toString().trim();

        if (nome.isEmpty() || dosagem.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        // Corrigido para usar a API não depreciada
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        calendar.set(Calendar.MINUTE, timePicker.getMinute());
        calendar.set(Calendar.SECOND, 0);
        Date horario = calendar.getTime();

        Medicamento medicamento = new Medicamento(nome, dosagem, horario);

        if (isEditMode) {
            cancelarAlarme(medicamentoAtual.getId());
            db.collection("medicamentos").document(medicamentoAtual.getId())
                    .set(medicamento)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Sucesso ao ALTERAR no Firebase.");
                        Toast.makeText(this, "Medicamento alterado!", Toast.LENGTH_SHORT).show();
                        agendarAlarme(medicamento, medicamentoAtual.getId());
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Falha ao ALTERAR no Firebase: ", e);
                        Toast.makeText(this, "Erro ao alterar.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("medicamentos").add(medicamento)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Sucesso ao SALVAR no Firebase.");
                        Toast.makeText(this, "Medicamento salvo!", Toast.LENGTH_SHORT).show();
                        agendarAlarme(medicamento, documentReference.getId());
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Falha ao SALVAR no Firebase: ", e);
                        Toast.makeText(this, "Erro ao salvar.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void excluirMedicamento() {
        Log.d(TAG, "Método excluirMedicamento() chamado.");
        if (medicamentoAtual == null || medicamentoAtual.getId() == null) {
            Log.d(TAG, "excluirMedicamento: medicamentoAtual é nulo, retornando.");
            return;
        }
        cancelarAlarme(medicamentoAtual.getId());
        db.collection("medicamentos").document(medicamentoAtual.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sucesso ao EXCLUIR no Firebase.");
                    Toast.makeText(this, "Medicamento excluído!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao EXCLUIR no Firebase: ", e);
                    Toast.makeText(this, "Erro ao excluir.", Toast.LENGTH_SHORT).show();
                });
    }

    private void agendarAlarme(Medicamento medicamento, String docId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager não encontrado.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Permissão para alarmes exatos não concedida.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Não foi possível agendar o alarme, permissão de alarme exato negada.");
            return;
        }

        int requestCode = docId.hashCode();
        Log.d(TAG, "Agendando alarme com requestCode: " + requestCode);

        Intent intent = new Intent(this, AlarmeReceiver.class);
        intent.putExtra("NOME_MEDICAMENTO", medicamento.getNome());
        intent.putExtra("NOTIFICATION_ID", requestCode);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(medicamento.getHorario());
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        intent.putExtra("ORIGINAL_TIME", calendar.getTimeInMillis());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Alarme agendado para: " + calendar.getTime().toString());
        Toast.makeText(this, "Alarme agendado!", Toast.LENGTH_SHORT).show();
    }

    private void cancelarAlarme(String docId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int requestCode = docId.hashCode();
        Log.d(TAG, "Cancelando alarme com requestCode: " + requestCode);

        Intent intent = new Intent(this, AlarmeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Alarme cancelado para o ID: " + docId);
        }
    }
}

