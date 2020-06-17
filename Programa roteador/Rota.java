public class Rota {
    public String destino; //rede
    public int mascara; //Formato CIDR
    public String gateway; //ip
    public String saida; //indicar porta que o roteador ta rodando ( interface )


    public Rota(String unparsedRota) {
        //parse
        String delims = "[/]";
        String[] tokens = unparsedRota.split(delims);

        this.destino = tokens[0];

        //formato extendido
        if (tokens[1].contains(".")) {
            this.mascara = parseMascara(tokens[1]);
        }
        //formato CIDR
        else {
            this.mascara = Integer.parseInt(tokens[1]);
        }

        this.gateway = tokens[2];
        this.saida = tokens[3];
    }

    //parse da string de mascara 255.255.0.0 (exemplo)
    private static int parseMascara(String mascara){
        int nmrMask = 0;
        String delims = "[.]+";
        String[] tokens = mascara.split(delims);

        for (int i=0; i < tokens.length ; i++){
            if (tokens[i].equals("255")){
                nmrMask += 8;
            } else {
                nmrMask += descobrirMascara(tokens[i]);
                break;
            }
        }
        return  nmrMask;
    }

    //descobre quantos bits do octeto estão sendo usados para mascara
    private static int descobrirMascara(String octeto){
        int nmrMask = 0;
        int x = Integer.parseInt(octeto); //TODO Logaritmo para ver se o octeto está na base 2
        String cba = Integer.toBinaryString(x);
        for (int i=0; i <cba.length() ; i++){
            if (cba.charAt(i) == '1'){
                nmrMask++;
            } else { break; }
        }
        return nmrMask;
    }
}