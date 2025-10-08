package com.cristian.meucontrolemed;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "MeuControleMedDebug";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "BootReceiver: Dispositivo reiniciado. A reagendar alarmes...");
            Toast.makeText(context, "Reagendando alarmes de medicação...", Toast.LENGTH_SHORT).show();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("medicamentos")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Medicamento medicamento = document.toObject(Medicamento.class);
                                medicamento.setId(document.getId());
                                reagendarAlarme(context, medicamento);
                            }
                        } else {
                            Log.e(TAG, "BootReceiver: Erro ao obter medicamentos para reagendamento.", task.getException());
                        }
                    });
        }
    }

    private void reagendarAlarme(Context context, Medicamento medicamento) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int requestCode = medicamento.getId().hashCode();
        Intent intent = new Intent(context, AlarmeReceiver.class);
        intent.putExtra("NOME_MEDICAMENTO", medicamento.getNome());
        intent.putExtra("NOTIFICATION_ID", requestCode);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(medicamento.getHorario());
        calendar.set(Calendar.SECOND, 0);

        // Se a hora já passou hoje, agenda para amanhã
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        intent.putExtra("ORIGINAL_TIME", calendar.getTimeInMillis());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "BootReceiver: Não foi possível reagendar, permissão de alarme exato negada.");
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "BootReceiver: Alarme reagendado para " + medicamento.getNome() + " às " + calendar.getTime().toString());
    }
}

