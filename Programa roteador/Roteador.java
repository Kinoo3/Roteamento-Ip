import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Roteador {
    public int port;
    List<Rota> rotas = new ArrayList<Rota>();

    public Roteador(int port){
        this.port = port;
    }

    public void adicionarRota(String rota){
        Rota novaRota = new Rota(rota);
        rotas.add(novaRota);
    }

    //responsável por aplicar a mascara de bits dado um byte e o número de bits que devem ser setados como 0 ( da direita para esquerda)
    public static byte aplicarmascara(byte octeto, int a){
        String s1 = String.format("%8s", Integer.toBinaryString(octeto & 0xFF)).replace(' ', '0');

        s1 = s1.substring(0,a);

        for(int i=0; i < 8-a; i++){
            s1 += "0";
        }
        short result = Short.parseShort(s1,2);
        byte resultado = (byte) result;
        return resultado;
    }

    //junta os dois bytes responsáveis pelo tamanho no pacote ip
    public static int calculaTamanho(byte maisSignificativo, byte menosSignificativo){
        byte highByte = maisSignificativo;
        byte lowByte = menosSignificativo;
        int tamanho = (highByte & 0xff) | ((lowByte & 0xff) << 8);
        return tamanho;
    }

    //aplica a mascara dado um endereço em formato de bytes, e retorna a string do endereço
    public static String rotaExtendidaMascaraAplicada (int mascara, byte desma1, byte desma2, byte desma3, byte desma4){
        if(mascara % 8 != 0){
            System.out.println("Mascara no formato CIDR nao é um múltiplo de 8, algo deu errado");
        }
        //Aplica a mascara ao ip de destino para poder executar o roteamento
        else if(mascara<=8){
            desma1 = Roteador.aplicarmascara(desma1, mascara);
            desma2 = (byte) 0;
            desma3 = (byte) 0;
            desma4 = (byte) 0;
        }
        else if(mascara<=16){
            desma2 = Roteador.aplicarmascara(desma2, mascara-8);
            desma3 = (byte) 0;
            desma4 = (byte) 0;
        }
        else if(mascara<=24){
            desma3 = Roteador.aplicarmascara(desma3, mascara-16);
            desma4 = (byte) 0;
        }
        else if(mascara<=32){
            desma4 = Roteador.aplicarmascara(desma4, mascara-24);
        }

        System.out.println("\nmascara aplicada");

        //garante que os ips estejam unsigned
        int destinoUnsigned1 = desma1 & 0xFF;
        int destinoUnsigned2 = desma2 & 0xFF;
        int destinoUnsigned3 = desma3 & 0xFF;
        int destinoUnsigned4 = desma4 & 0xFF;

        String resultado = destinoUnsigned1 + "." + destinoUnsigned2 + "." + destinoUnsigned3 + "." + destinoUnsigned4;
        return resultado;
    }


    public static String geraRotaFormatoExtendido(byte octeto1, byte octeto2, byte octeto3, byte octeto4){
        //String que forma a rota no formato 140.23.0.4, assim como a rota que vem por ARGS pro roteador.
        return  octeto1 + "." + octeto2 + "." + octeto3 + "." + octeto4;
    }


    public static void main(String[] args) throws IOException {
        int port;

        try {
            port = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e)
        {
            port = 0;
            System.out.println("Argumento para porta não foi um inteiro! Porta inicializada como 0");
        }

        Roteador roteador = new Roteador(port);

        //cria todas rotas atribuidas ao roteador
        //parse da string da rota (/)
        for (int i=1; i < args.length; i++){
            roteador.adicionarRota(args[i]);
        }

        DatagramSocket serverSocket;

        InetAddress addr = InetAddress.getByName("127.0.0.1");
        InetSocketAddress add = new InetSocketAddress(addr, port);
        serverSocket = new DatagramSocket(null);
        serverSocket.setReuseAddress(true);
        serverSocket.bind(add);


        System.out.println("Roteador rodando na porta: " + serverSocket.getLocalPort());
        while (true) {

            byte[] receiveData = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(packet);
            byte[] data = packet.getData();

            //por octeto
            byte destino1, destino2, destino3, destino4;
            destino1 = data[16];
            destino2 = data[17];
            destino3 = data[18];
            destino4 = data[19];

            String rotaDestinoFormatoExtendido = geraRotaFormatoExtendido(data[16], data[17], data[18], data[19]);

            int timetolive = data[8];
            if(timetolive<=1){
                System.out.println("time to live expirou, descartando o pacote de destino: " + rotaDestinoFormatoExtendido);
            }
            else {
                timetolive = timetolive - 1;
                data[8] = (byte) timetolive;

                //rota inválida utilizada para confirmação após loop
                Rota rotaSaida = new Rota("266.266.266.266/266.266.266.266/266.266.266.266/266");

                int prefixoSize = 0;

                //Percorre as rotas
                //aplica a mascara de cada rota ao destino
                for (Rota rota : roteador.rotas) {
                    String rotaFormatoExtendido = Roteador.rotaExtendidaMascaraAplicada(rota.mascara, destino1, destino2, destino3, destino4);

                    if (rotaFormatoExtendido.equals(rota.destino)) {
                        System.out.println("match com rota: ");
                        System.out.println(rota.destino);
                        System.out.println("com prefixo: " + rota.mascara);

                        //Garante que o maior prefixo seja usado
                        if (rota.mascara >= prefixoSize) {
                            rotaSaida = rota;
                            prefixoSize = rota.mascara;
                        }
                    } else {
                        System.out.println("unmatch com rota: " + rota.destino + "\n");
                    }
                }


                //significa que não achou nenhuma match, nem mesmo default
                if(rotaSaida.destino.equals("266.266.266.266")){
                    System.out.println("Destino " + rotaDestinoFormatoExtendido + "não encontrado na tabela de roteamento, descartando pacote");
                }
                else {
                    if (rotaSaida.gateway.equals("0.0.0.0")) {
                        int tamanho = Roteador.calculaTamanho(data[2], data[3]);
                        String mensagem = new String(data, 20, tamanho);

                        String rotaOrigemFormatoExtendido = Roteador.geraRotaFormatoExtendido(data[12], data[13], data[14], data[15]);

                        System.out.println("pacote chegou, de: " + rotaOrigemFormatoExtendido + "\nate: " + rotaDestinoFormatoExtendido);
                        System.out.println(mensagem);

                    } else {
                        int portToSend = Integer.parseInt(rotaSaida.saida);
                        InetAddress address = InetAddress.getByName(rotaSaida.gateway);

                        System.out.println("mandando pacote para: " + rotaDestinoFormatoExtendido + "   pelo next hop: " + rotaSaida.gateway + "    pela interface: " + rotaSaida.saida);

                        DatagramPacket packetToSend = new DatagramPacket(data, data.length, address, portToSend);
                        DatagramSocket client = new DatagramSocket();
                        client.send(packetToSend);
                    }
                }
            }

        }
    }
}
