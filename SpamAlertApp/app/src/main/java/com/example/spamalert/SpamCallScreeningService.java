package com.example.spamalert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.telecom.CallScreeningService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SpamCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details callDetails) {
        String incomingNumber = callDetails.getHandle().getSchemeSpecificPart();

        new Thread(() -> checkNumberAndNotify(incomingNumber)).start();

        CallResponse response = new CallResponse.Builder()
                .setDisallowCall(false)
                .setSilenceCall(false)
                .build();
        respondToCall(callDetails, response);
    }

    private void checkNumberAndNotify(String number) {
        try {
            String url = "https://www.listaspam.com/busca.php?Telefono=" + number;
            Document doc = Jsoup.connect(url).get();

            Elements owner = doc.select("div.phone-owner");
            Elements reportCount = doc.select("div.n_reports span.result");

            boolean spamDetected = !reportCount.isEmpty() && !reportCount.text().equals("0");
            String ownerName = owner.isEmpty() ? "Desconocido" : owner.text();

            String msg = spamDetected ?
                    "⚠️ Llamada sospechosa: " + number + "\nOrigen: " + ownerName :
                    "✅ Número " + number + " no está en la lista de SPAM";

            showNotification(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "spam_alert_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Spam Alert", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("SpamAlertApp")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}
