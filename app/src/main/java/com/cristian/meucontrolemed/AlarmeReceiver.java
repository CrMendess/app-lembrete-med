package com.cristian.meucontrolemed;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class AlarmeReceiver extends BroadcastReceiver {
    private static final String TAG = "MeuControleMedDebug";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmeReceiver: onReceive foi chamado!");

        String nomeMedicamento = intent.getStringExtra("NOME_MEDICAMENTO");
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);
        long originalTime = intent.getLongExtra("ORIGINAL_TIME", 0);

        if (nomeMedicamento == null) {
            Log.e(TAG, "Nome do medicamento é nulo, não é possível criar notificação.");
            return;
        }

        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "id_canal_remedio")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Hora do Remédio!")
                .setContentText("Não se esqueça de tomar seu " + nomeMedicamento)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Sem permissão para postar notificações no AlarmeReceiver.");
            return;
        }
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notificação exibida para: " + nomeMedicamento);

        if (originalTime > 0) {
            reagendarAlarmeParaProximoDia(context, intent, notificationId);
        }
    }

    private void reagendarAlarmeParaProximoDia(Context context, Intent intent, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent newIntent = new Intent(context, AlarmeReceiver.class);
        newIntent.putExtras(intent.getExtras());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, newIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(newIntent.getLongExtra("ORIGINAL_TIME", 0));
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        newIntent.putExtra("ORIGINAL_TIME", calendar.getTimeInMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Não foi possível reagendar o alarme, permissão negada.");
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Alarme REAGENDADO para o dia seguinte: " + calendar.getTime().toString());
    }
}

