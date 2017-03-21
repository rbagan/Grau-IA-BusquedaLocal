package practicaia;

import IA.Red.Centro;
import IA.Red.CentrosDatos;
import IA.Red.Sensor;
import IA.Red.Sensores;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class PracticaIAEstado {
    public static final int MAX_CONEXIONES_SENSORES = 3;
    public static final int MAX_CONEXIONES_CENTROS = 25;
    
    private static int NUM_SENSORES;   
    private static int NUM_CENTROS;
    
    private static double[][] matrizDistanciasEntreSensores;
    private static double[][] matrizDistanciasSensoresACentro;
    private static CentrosDatos centros;
    private static Sensores sensores;
    
    private int[] sensorDestino;
    private HashMap<Integer,HashSet<Integer>> redSensores;
    private HashMap<Integer,HashSet<Integer>> redCentros;

    PracticaIAEstado(PracticaIAEstado estado) {
        this.redSensores = PracticaIAEstado.copiaRed(estado.redSensores);
        this.redCentros = PracticaIAEstado.copiaRed(estado.redCentros);
        this.sensorDestino = estado.sensorDestino.clone();
    }
    
    private static HashMap<Integer, HashSet<Integer>> copiaRed(HashMap<Integer, HashSet<Integer>> original) {
        HashMap<Integer, HashSet<Integer>> copia = new HashMap<>();
        for (Integer key : original.keySet()) {
            copia.put(key, new HashSet<>());
            
            for (Integer value : original.get(key)) {
                copia.get(key).add(value);
            }            
        }
        
        return copia;
    }
    
    private double calcularDistancia(Sensor a, Sensor b) {
        return Math.sqrt(Math.pow(a.getCoordX() - b.getCoordX(), 2) 
               + Math.pow(a.getCoordY() - b.getCoordY(), 2));
    }
    
    private double calcularDistancia(Sensor s, Centro c) {
        return Math.sqrt(Math.pow(s.getCoordX() - c.getCoordX(), 2) 
               + Math.pow(s.getCoordY() - c.getCoordY(), 2));
    }
    
    private void calcularDistanciasEntreSensores(){
        for(int i = 0; i < NUM_SENSORES; ++i) {
            for(int j = 0; j < NUM_SENSORES; ++j) {
                matrizDistanciasEntreSensores[i][j] = calcularDistancia(sensores.get(i), sensores.get(j));
            }
        }
    }
    
    private void calcularDistanciasSensoresACentro() {
        for(int i = 0; i < NUM_SENSORES; ++i) {
            for(int j = 0; j < NUM_CENTROS; ++j) {
                matrizDistanciasSensoresACentro[i][j] = calcularDistancia(sensores.get(i), centros.get(j));
            }
        }
    }
    
    private void inicializarRed() {
        for (int i = 0; i < NUM_SENSORES; ++i) {
            this.redSensores.put(i, new HashSet<>());
        }
        
        for (int i = 0; i < NUM_CENTROS; ++i) {
            this.redCentros.put(i + NUM_SENSORES, new HashSet<>());
        }
    }
    
    public boolean hayCiclos(int origen) {
        int aux = origen;
        while(!esCentro(sensorDestino[aux])){
            aux = sensorDestino[aux];
            if(aux == origen)
                return true;
        }
        return false;
    } 
        
    public double calcularDistanciaSubArbolSensor(int indiceSensor) {
        double distancia = 0.0;
        
        for (Integer s : this.redSensores.get(indiceSensor)) {
            distancia = distancia + matrizDistanciasEntreSensores[indiceSensor][s];
            distancia = distancia + calcularDistanciaSubArbolSensor(s);
        }
        
        return distancia;
    }
    
    public double calcularDistanciaCentroIessimo(int indiceCentro) {
        double distancia = 0.0;
        indiceCentro = indiceCentro + NUM_SENSORES;
        
        for (Integer s : this.redCentros.get(indiceCentro)) {
            distancia = distancia + matrizDistanciasSensoresACentro[s][indiceCentro - NUM_SENSORES]; 
            distancia = distancia + calcularDistanciaSubArbolSensor(s);
        }
        
        return distancia;
    }
    
    public PracticaIAEstado(Sensores sensores, CentrosDatos centros) {
        NUM_SENSORES = sensores.size();
        NUM_CENTROS = centros.size();
        
        this.centros = centros;
        this.sensores = sensores;
        this.redSensores = new HashMap<>();
        this.redCentros = new HashMap<>();
        inicializarRed();
        
        this.sensorDestino = new int[NUM_SENSORES];
        Arrays.fill(sensorDestino, -1);        
        
        matrizDistanciasEntreSensores = new double[NUM_SENSORES][NUM_SENSORES];
        calcularDistanciasEntreSensores();
        
        matrizDistanciasSensoresACentro = new double[NUM_SENSORES][NUM_CENTROS];
        calcularDistanciasSensoresACentro();
    }
        
    public void generarEstadoInicial() {
        int indiceSensor = 0;
        int indiceCentro = 0;
        while (indiceSensor < NUM_SENSORES && aceptaConexion(NUM_SENSORES + NUM_CENTROS - 1)) {            
            this.redCentros.get(indiceCentro + NUM_SENSORES).add(indiceSensor);
            
            ++indiceSensor;
            indiceCentro = ++indiceCentro % NUM_CENTROS;
        }
        
        int offset = NUM_CENTROS * MAX_CONEXIONES_CENTROS;
        int j = 1;
        int i = 0;
        
        while (indiceSensor < NUM_SENSORES) {
            if (this.redSensores.get(offset * j).size() >= MAX_CONEXIONES_SENSORES) {
                j = j * MAX_CONEXIONES_SENSORES;
            }
            
            i = (i % (offset * j)) + ((j == 1) ? 0 : offset * (j/3));
            
            this.redSensores.get(i).add(indiceSensor);
            
            ++indiceSensor;
            ++i;
        }
    }
    
    //conectar sensorA a sensorB
    public boolean mover(int sensor, int destino) {        
        int destinoAnterior = sensorDestino[sensor];
        sensorDestino[sensor] = destino;
        
        if(!movimientoValido(sensor))
        {
            return false;
        }
        else
        {
            desconectarSensorAEnB(sensor, destinoAnterior);
            conectarSensorAEnB(sensor, destino);
        }
        
       return true;
    }
    
    //intercambiar sensorA por sensorB y viceversa
    public boolean intercambiar(int sensorA, int sensorB){
        int destinoA = sensorDestino[sensorA];
        return mover(sensorA, sensorDestino[sensorB]) 
                && mover(sensorB, destinoA);
    }
    
    private void desconectarSensorAEnB(int sensor, int destino) {
        if(esCentro(destino))
        {
            this.redCentros.get(getCentroId(destino)).remove(sensor);
        }
        else
        {
            this.redSensores.get(destino).remove(sensor);
        }
    }
    private void conectarSensorAEnB(int sensor, int destino) {
        if(esCentro(destino))
        {
            this.redCentros.get(getCentroId(destino)).add(sensor);
        }
        else
        {
            this.redSensores.get(destino).add(sensor);
        }
    }
   
    
    public boolean movimientoValido(int sensor) {
        int destino = sensorDestino[sensor];
        return aceptaConexion(destino) &&
           !hayCiclos(destino);
    }
    
    public int getCentroId(int indiceAbsoluto){
        return indiceAbsoluto - NUM_SENSORES;
    }
    
    public boolean sensorAceptaConexion(int indiceSensor) {
        return this.redSensores.get(indiceSensor).size() < MAX_CONEXIONES_SENSORES;        
    }
    
    public boolean centroAceptaConexion(int indiceCentro) {
        return this.redCentros.get(indiceCentro).size() < MAX_CONEXIONES_CENTROS;
    }
    
    private boolean aceptaConexion(int sensor) {
        if(esCentro(sensor))
        {
            return centroAceptaConexion(sensor);
        }
        else
        {
            return sensorAceptaConexion(sensor);
        }
    }
        
    private boolean esCentro(int destino) {
        return destino >= NUM_SENSORES;
    }
    
    public static int getNUM_SENSORES() {
        return NUM_SENSORES;
    }

    public static int getNUM_CENTROS() {
        return NUM_CENTROS;
    }

    /// Metodos Debug
    
    public void debugPrintMatrizDistancias() {
        System.out.println();
        System.out.println("Matriz de distancias entre sensores: ");
        System.out.print("\t ");
        for (int i = 0; i < NUM_SENSORES; ++i) {
            if ((i+1) > 9) {
                System.out.print("S" + (i+1) + "    ");
            }
            else 
            {
                System.out.print("S" + (i+1) + "     ");
            }
        }
        System.out.println();
        for (int i = 0; i < 100; ++i) {
            System.out.print("-");
        }
        System.out.println();
        for (int i = 0; i < NUM_SENSORES; ++i) {
            System.out.print("S" + (i + 1) + "\t|");
            for (int j = 0; j < NUM_SENSORES; ++j) {
                System.out.format("%.4f ", matrizDistanciasEntreSensores[i][j]);
            }
            System.out.println();
        }
    }
    
    public void debugPrintMatrizSensorACentro() {   
        System.out.println();     
        System.out.println("Matriz de distancias sensores a centros: ");
        System.out.print("\t ");
        for (int i = 0; i < NUM_CENTROS; ++i) {
            if ((i+1) > 9) {
                System.out.print("C" + (i+1) + "    ");
            }
            else 
            {
                System.out.print("C" + (i+1) + "     ");
            }
        }
        System.out.println();
        for (int i = 0; i < 100; ++i) {
            System.out.print("-");
        }
        System.out.println();
        for (int i = 0; i < NUM_SENSORES; ++i) {
            System.out.print("S" + (i + 1) + "\t|");
            for (int j = NUM_SENSORES; j < NUM_SENSORES + NUM_CENTROS; ++j) {
                System.out.format("%.4f ", matrizDistanciasSensoresACentro[i][j-NUM_SENSORES]);
            }
            System.out.println();
        }
    }
    
    public void debugPrintSensores() {
        System.out.println();
        System.out.println("Informacion de los sensores: ");
        for (int i = 0; i < NUM_SENSORES; ++i) {
            System.out.println("\tSensor " + (i + 1));
            System.out.println("\tCapacidad: " + sensores.get(i).getCapacidad());
            System.out.println("\tPosicion (X,Y): (" + 
                    sensores.get(i).getCoordX() + "," + 
                    sensores.get(i).getCoordY() + ")");
            System.out.println();
        }
    }
    
    public void debugPrintCentros() {
        System.out.println();
        System.out.println("Informacion de los centros: ");
        for (int i = 0; i < NUM_CENTROS; ++i) {
            System.out.println("\tCentro " + (i + 1));
            System.out.println("\tPosicion (X,Y): (" + 
                    centros.get(i).getCoordX() + "," + 
                    centros.get(i).getCoordY() + ")");
            System.out.println();
        }
    }
    
    private void debugPrintRed_i(int indice, int tab) {
        for (int i = 0; i < tab; ++i) {
            System.out.print("\t");
        }
        System.out.println("Sensor " + (indice + 1) + ": ");
        for (Integer s : this.redSensores.get(indice)) {
            debugPrintRed_i(s,++tab);
        }
    }
    
    public void debugPrintRed() {
        System.out.println();
        System.out.println("Informacion de la red: ");
        for (int i = 0; i < NUM_CENTROS; ++i) {
            System.out.println("##############################################");
            System.out.println("Centro " + (i + 1) + ", tiene las siguientes conexiones: ");
            for (Integer s : this.redCentros.get(i + NUM_SENSORES)) {
                debugPrintRed_i(s, 1);
            }
            System.out.println("##############################################");
        }
    }    
}