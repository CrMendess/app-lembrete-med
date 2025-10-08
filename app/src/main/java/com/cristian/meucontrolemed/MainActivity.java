package com.cristian.meucontrolemed;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MeuControleMedDebug";
    private RecyclerView recyclerView;
    private MedicamentoAdapter adapter;
    private List<Medicamento> listaMedicamentos;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Permissão de NOTIFICAÇÃO concedida.");
                    verificarPermissaoAlarmeExato();
                } else {
                    Log.w(TAG, "Permissão de NOTIFICAÇÃO negada.");
                    Toast.makeText(this, "Permissão de notificação é necessária para os lembretes.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewMedicamentos);
        FloatingActionButton fab = findViewById(R.id.fabAdicionar);

        fab.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, AdicionarMedicamentoActivity.class));
        });

        setupRecyclerView();
        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pedirPermissoesSequencialmente();
        carregarMedicamentos();
    }

    private void setupRecyclerView() {
        listaMedicamentos = new ArrayList<>();
        adapter = new MedicamentoAdapter(listaMedicamentos, medicamento -> {
            Intent intent = new Intent(MainActivity.this, AdicionarMedicamentoActivity.class);
            intent.putExtra("medicamento_para_editar", medicamento);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void carregarMedicamentos() {
        db.collection("medicamentos")
                .orderBy("horario", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Erro ao carregar medicamentos: ", e);
                        return;
                    }
                    if (snapshots != null) {
                        listaMedicamentos.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Medicamento med = doc.toObject(Medicamento.class);
                            med.setId(doc.getId());
                            listaMedicamentos.add(med);
                        }
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Lista de medicamentos carregada com " + listaMedicamentos.size() + " itens.");
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Canal Lembrete Remédio";
            String description = "Canal para notificações de medicamentos";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("id_canal_remedio", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void pedirPermissoesSequencialmente() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "A pedir permissão de NOTIFICAÇÃO.");
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Permissão de notificação já concedida. A verificar alarme...");
                verificarPermissaoAlarmeExato();
            }
        } else {
            Log.d(TAG, "Versão do Android < 13. A verificar alarme...");
            verificarPermissaoAlarmeExato();
        }
    }

    private void verificarPermissaoAlarmeExato() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "A pedir permissão de ALARME EXATO.");
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Por favor, autorize o app a agendar alarmes e lembretes.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Permissão de alarme exato já concedida.");
            }
        } else {
            Log.d(TAG, "Versão do Android < 12. Não é preciso permissão de alarme exato.");
        }
    }
}

