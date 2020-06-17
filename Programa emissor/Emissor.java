import java.io.IOException;
import java.net.*;

public class Emissor {
    public static void main(String[] args) throws IOException {

        System.out.println(args[0]);//ip roteador default
        System.out.println(args[1]);//porta roteador default
        System.out.println(args[2]);//ip origem
        System.out.println(args[3]);//ip destino
        System.out.println(args[4]);//mensagem

        InetAddress address = InetAddress.getByName(args[0]);

        int port;
        try {
            port = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            port = 0;
            System.out.println("Argumento para porta não foi um inteiro!");
        }

        PacoteIP pacoteIP = new PacoteIP(args[2], args[3], args[4]);

        DatagramPacket packetToSend = new DatagramPacket(pacoteIP.pacoteFinal, pacoteIP.pacoteFinal.length, address, port);
        DatagramSocket client = new DatagramSocket();
        client.send(packetToSend);
    }
}
