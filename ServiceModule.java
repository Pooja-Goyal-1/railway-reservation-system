import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.*;

class QueryRunner implements Runnable
{
    //  Declare socket for client access
    protected Socket socketConnection;
    Connection dbConnection;
   
    public QueryRunner(Socket clientSocket, Connection dbConnection)
    {
        this.socketConnection =  clientSocket;
        this.dbConnection = dbConnection;
    }

    public void run()
    {
      try
        {
            //  Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                                                                  .getInputStream()) ;
            BufferedReader bufferedInput = new BufferedReader(inputStream) ;
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                                                                     .getOutputStream()) ;
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream) ;
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true) ;
            Statement stmt = null;
            String clientCommand = "" ;

            // Read client query from the socket endpoint
            clientCommand = bufferedInput.readLine(); 

            while( ! clientCommand.equals("#"))
            {
                try
                {
                    stmt = dbConnection.createStatement();
                    String[] words=clientCommand.split("\\s");

                    if(words.length == 2){
                        String runfunction = "{ ?=call search(? , ?  ) }";
                        CallableStatement callstmnt = dbConnection.prepareCall(runfunction);
                    
                        callstmnt.registerOutParameter(1, Types.VARCHAR);
                        callstmnt.setString(2, words[0]);
                        
                        callstmnt.setString(3, words[1]);
                        
                        callstmnt.execute();
                        String res = callstmnt.getString(1);
                        
                        String[] finalOutput = res.split("\\?");
    
                        for(int i =0;i<finalOutput.length;i++){
                            printWriter.println(finalOutput[i]);
                        }
                    }
                    else if(words.length == 4){

                        String runfunction = "{ call release_train(? , ? , ?, ? ) }";
                        
                        CallableStatement callstmnt = dbConnection.prepareCall(runfunction);
                        
                        callstmnt.setInt(1, Integer.parseInt(words[0]));
                         
                        callstmnt.setDate(2, Date.valueOf(words[1]));
                        callstmnt.setInt(3, Integer.parseInt(words[2]));
                        callstmnt.setInt(4, Integer.parseInt(words[3]));
                        
                        callstmnt.execute();
                        
                        printWriter.println("Train released");
                    }
                    else{
                        String runfunction = "{ ? = call book_tickets(? , ? , ?, ? ,?) }";
                        
                        CallableStatement callstmnt = dbConnection.prepareCall(runfunction);
                        callstmnt.registerOutParameter(1, Types.VARCHAR);
                        Integer i = Integer.parseInt(words[0]);
                        callstmnt.setInt(2, i);
                        callstmnt.setString(3, words[1]);
                        callstmnt.setInt(4, Integer.parseInt(words[1+i]));
                        callstmnt.setDate(5, Date.valueOf(words[2+i]));
                        callstmnt.setString(6, words[3+i]);
                        
                        callstmnt.execute();

                        String res = callstmnt.getString(1);

                        if(res.equals("1")){
                            printWriter.println("TRAIN NOT AVAILABLE\n");
                        }
                        else if(res.equals("2")){
                            printWriter.println("SEATS NOT AVAILABLE\n");
                        }
                        else {
                            printWriter.println("TICKET BOOKED:: PNR: " + res + " Train No: " + words[1+i] + " Date of journey: " + words[2+i]);

                            // print ticket
                            int len = res.length();
                            String berth_no = res.substring(len-2,len);
                            int temp = words[1+i].length() + 9;
                            String coach_no = res.substring(temp, len-2);
                            Integer berth = Integer.parseInt(berth_no);
                            String berth_type ="" ;
                            if(words[3+i].equals("AC")){
                                if(berth%6==1 || berth%6==2){
                                    berth_type="LB";
                                }
                                else if(berth%6==3 || berth%6==4){
                                    berth_type="UB";
                                }
                                else if(berth%6==5){
                                    berth_type="SL";
                                }
                                else{
                                    berth_type="SU";
                                }
                            }

                            if(words[3+i].equals("SL")){
                                if(berth%8==1 || berth%8==4){
                                    berth_type="LB";
                                }
                                else if(berth%8==2 || berth%8==5){
                                    berth_type="MB";
                                }
                                else if(berth%8==3 || berth%8==6){
                                    berth_type="UB";
                                }
                                else if(berth%8==7){
                                    berth_type="SL";
                                }
                                else{
                                    berth_type="SU";
                                }
                            }
                            Integer coach = Integer.parseInt(coach_no);
                            coach++;
                            berth++;

                            for(int j = 1;j<=i;j++){

                                if(words[3+i].equals("AC")){
                                    if(berth%6==1 || berth%6==2){
                                        berth_type="LB";
                                    }
                                    else if(berth%6==3 || berth%6==4){
                                        berth_type="UB";
                                    }
                                    else if(berth%6==5){
                                        berth_type="SL";
                                    }
                                    else{
                                        berth_type="SU";
                                    }
                                }
    
                                if(words[3+i].equals("SL")){
                                    if(berth%8==1 || berth%8==4){
                                        berth_type="LB";
                                    }
                                    else if(berth%8==2 || berth%8==5){
                                        berth_type="MB";
                                    }
                                    else if(berth%8==3 || berth%8==6){
                                        berth_type="UB";
                                    }
                                    else if(berth%8==7){
                                        berth_type="SL";
                                    }
                                    else{
                                        berth_type="SU";
                                    }
                                }

                                if(j!=i){
                                    String name = words[j].substring(0,words[j].length()-1);
                                    printWriter.println("Name: "+name+" Coach Number: "+String.valueOf(coach)+ " Berth Number: "+String.valueOf(berth)+" Berth Type: "+berth_type);
                                }
                                else{
                                    printWriter.println("Name: "+words[j]+" Coach Number: "+String.valueOf(coach)+ " Berth Number: "+String.valueOf(berth)+" Berth Type: "+berth_type+"\n");
                                }
                                berth++;
                                if(words[3+i].equals("AC") && berth==19){
                                    berth = 1;
                                    coach ++;
                                }
                                if(words[3+i].equals("SL") && berth==25){
                                    berth = 1;
                                    coach++;
                                }
                            }
                            
                        }

                   stmt.close();

                    } 
                }
                catch (Exception e)  // InterruptedException replaced by Exception 
                {
                    e.printStackTrace();
                }
                
                // Read next client query
                clientCommand = bufferedInput.readLine(); 
            }
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        }
        catch(IOException e)
        {
            return;
        }
    }
}

/**
 * Main Class to controll the program flow
*/
public class ServiceModule 
{
    // Server listens to port
    static int serverPort = 7008;
    // Max no of parallel requests the server can process
    static int numServerCores = 5 ;         
    //------------ Main----------------------
    static final String JDBC_DRIVER = "org.postgresql.Driver";
    static final String DB_URL = "jdbc:postgresql://localhost/railwayreservationsystem";

    // Databse Credentials
    static final String USER = "postgres";
    static final String PASSWORD = "0921";
    public static void main(String[] args) throws IOException 
    {    
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);
        
        try (//Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort)) {
            Socket socketConnection = null;
            Connection dbConnection = null;
            // Always-ON server
            while(true)
            {
                System.out.println("Listening port : " + serverPort 
                                    + "\nWaiting for clients...");

                try{dbConnection = DriverManager.getConnection(DB_URL, USER, PASSWORD);} catch (Exception e) {
                    System.out.println(e.getClass().getName()+": "+e.getMessage());
                    return;
                }
                socketConnection = serverSocket.accept();   // Accept a connection from a client
                System.out.println("Accepted client :" 
                                    + socketConnection.getRemoteSocketAddress().toString() 
                                    + "\n");
                //  Create a runnable task
                Runnable runnableTask = new QueryRunner(socketConnection,dbConnection);
                //  Submit task for execution   
                executorService.submit(runnableTask);   
            }
        }
    }
}