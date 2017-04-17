/*
package gurobi;
import gurobi.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
public class MaxQbfacGurobi {
  private static int _size;  
  private static Double A[][];  
  public static void main(String[] args) {

    try {
      readInput("instances/qbf020");
        
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
      int totalVar = _size * _size;
      GRBVar[] variaveis = new GRBVar[totalVar];
      for (int i = 0; i < _size; ++i) {
        for(int j=0; j<_size; j++){          
            model.addVar(0, 1, A[i][j], GRB.BINARY, "X_" + i+ "X_"+j);
         
        }
      }
      
      model.optimize();

      int optimstatus = model.get(GRB.IntAttr.Status);

      if (optimstatus == GRB.Status.INF_OR_UNBD) {
        model.set(GRB.IntParam.Presolve, 0);
        model.optimize();
        optimstatus = model.get(GRB.IntAttr.Status);
      }

      if (optimstatus == GRB.Status.OPTIMAL) {
        double objval = model.get(GRB.DoubleAttr.ObjVal);        
        System.out.println("Optimal objective: " + objval);
      } else if (optimstatus == GRB.Status.INFEASIBLE) {
        System.out.println("Model is infeasible");

        // Compute and write out IIS
        model.computeIIS();
        model.write("model.ilp");
      } else if (optimstatus == GRB.Status.UNBOUNDED) {
        System.out.println("Model is unbounded");
      } else {
        System.out.println("Optimization was stopped with status = "
                           + optimstatus);
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    } catch(IOException e){
        System.out.println("Erro na leitura de arquivo: "+ e.getMessage());
    }
  }
  
  
  
  protected static Integer readInput(String filename) throws IOException {

		Reader fileInst = new BufferedReader(new FileReader(filename));
		StreamTokenizer stok = new StreamTokenizer(fileInst);

		stok.nextToken();
		_size = (int) stok.nval;
		A = new Double[_size][_size];

		for (int i = 0; i < _size; i++) {
			for (int j = i; j < _size; j++) {
				stok.nextToken();
				A[i][j] = stok.nval;				
				if (j>i)
					A[j][i] = 0.0;
			}
		}

		return _size;

	}
}
*/