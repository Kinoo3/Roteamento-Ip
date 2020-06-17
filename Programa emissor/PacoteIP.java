import java.math.BigInteger;
import java.nio.ByteBuffer;

public class PacoteIP {

    byte[] pacoteFinal;
    byte[] ipOrigem;
    byte[] ipDestino;
    byte[] tamanho = new byte[2];


    int versaoETamanhoCabecalho = 69; //0100 0101 ( 4 primeiros para setar IPV4 como versao, 4 últimos pro tamanho cabeçalho)
                                        //setado como minimum size de cabeçalho, 5*32 = 160 bits = 20 bytes, tamanho cabeçalho sem options

    int tipoServico = 32;   //001 0 0 0 00 , segundo RFC 791, isso da prioridade no campo precedência
                            //normal delay, normal throughput, normal relibity, e os dois ultimos reservados para uso futuro

    int ident = 0;


    int flagEflagOffset1 = 0, flagEflagOffset2 = 0;
    // para flags, sendo eles, em ordem, SEMUSO; MORE FRAGMENTOS; DONT FRAGMENT

    int timeToLive = 5; // 00000101

    int protocolo = 17; //valor para o UDP

    int checksum1 =0, checksum2 =0;

    byte[] dados; //dados podem variar, a mensagem é armazenada temporariamente aqui


    public PacoteIP(String origem, String destino, String mensagem){
        this.ipOrigem = ipToBinary(origem);
        this.ipDestino = ipToBinary(destino);

        dados = mensagem.getBytes();

        pacoteFinal = new byte[dados.length+20];

        this.setTamanho();

        this.setPacoteFinal();
    }

    //aloca um integer ao longo de 16 bits
    public void setTamanho(){
        int tamanhoDoPacote = 20 + dados.length;
        tamanho[0] = (byte)(tamanhoDoPacote & 0xff);
        tamanho[1] = (byte)((tamanhoDoPacote >> 8) & 0xff);
    }

    //recebe uma string no formato 255.255.0.0 e retorna o binario correspondente
    public byte[] ipToBinary(String ip){
        //parse
        String delims = "[.]+";
        String[] tokens = ip.split(delims);
        byte[] resultado = new byte[4];

        for(int i = 0; i<4; i++){
            resultado[i] = decimalStringToByte(tokens[i]);
        }
        return  resultado;
    }

    //byte correspondente a uma string na base decimal
    public byte decimalStringToByte(String string){
        int result = Integer.parseInt(string);
        return (byte) result;
    }

    //chamado ao fim do construtor, para construir totalmente o byte array que será enviado pelo datagrampacket
    public void setPacoteFinal(){
        pacoteFinal[0] = intToByte(versaoETamanhoCabecalho);
        pacoteFinal[1] = intToByte(tipoServico);
        pacoteFinal[2] = tamanho[0];
        pacoteFinal[3] = tamanho[1];
        pacoteFinal[4] = intToByte(ident);
        pacoteFinal[5] = intToByte(ident);
        pacoteFinal[6] = intToByte(flagEflagOffset1);
        pacoteFinal[7] = intToByte(flagEflagOffset2);
        pacoteFinal[8] = intToByte(timeToLive);
        pacoteFinal[9] = intToByte(protocolo);
        pacoteFinal[10] = intToByte(checksum1);
        pacoteFinal[11] = intToByte(checksum2);
        pacoteFinal[12] = ipOrigem[0];
        pacoteFinal[13] = ipOrigem[1];
        pacoteFinal[14] = ipOrigem[2];
        pacoteFinal[15] = ipOrigem[3];
        pacoteFinal[16] = ipDestino[0];
        pacoteFinal[17] = ipDestino[1];
        pacoteFinal[18] = ipDestino[2];
        pacoteFinal[19] = ipDestino[3];

        for(int i=0; i<dados.length; i++){
            pacoteFinal[i+20] = dados[i];
        }
    }

    public static byte intToByte(int valor) {
        if (valor >= 0 && valor <= 255) {
            return (byte) valor;
        }
        return (byte) 0;
    }
}
