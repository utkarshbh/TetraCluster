//******************************************************************************
//
// File:    TetraClu.java
// Using Package: edu.rit.pj2
//
// This Java source file is copyright (C) 2015 by Utkarsh Bhatia. All rights
// reserved. For further information, contact the author, Utkarsh Bhatia, at
// uxb9472@rit.edu.
//
// This class is extending Job class as given in the PJ2 parallel java library
// made by Professor Alan Kaminsky, the given reference to parallel java library and its sample
// code can be referenced from http://www.cs.rit.edu/~ark/bcbd/#source and http://www.cs.rit.edu/~ark/pj2.shtml
// This class is used for running the masterFor loop
// which works in multiple cluster machine. It also accepts variable cores which can be specified
// by the user. It makes use of the reduction variable which is reduced to the desired variable
// according to the user. It is using dynamic scheduling to schedule the numbers between input 
// range to various cores.
//
// Details for PJ2 library as available on http://www.cs.rit.edu/~ark/pj2.shtml
// The library has been made available to General Public under GPL license by 
// Professor Alan Kaminsky. The copyright (C) 2015 to pj2 library is held by Alan Kaminsky.
// PJ2 is free software; you can redistribute it and/or modify it under the terms of
// the GNU General Public License as published by the Free Software Foundation;
// either version 3 of the License, or (at your option) any later version.
//
// PJ2 is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.rit.io.InStream;
import edu.rit.io.OutStream;
import edu.rit.io.Streamable;
import edu.rit.pj2.Job;
import edu.rit.pj2.Loop;
import edu.rit.pj2.Task;
import edu.rit.pj2.Tuple;
import edu.rit.pj2.Vbl;
import edu.rit.pj2.tuple.ObjectArrayTuple;

/**
 * Class TertaClu is a multicluster parallel program that performs calculations to 
 * find the tetrahedron with the smallest volume.
 * It finds the tetrahedron with the smallest volume from the given cordinates. 
 * 
 * Using: java pj2 edu.rit.pj2 from http://www.cs.rit.edu/~ark/pj2.shtml
 * inputFile = Path for the input file containing the cordinates
 *
 * @author  Utkarsh Bhatia
 * @version 20-Oct-2015
 * 			The program is balancing the load at master level. 
 * 			The Tetrahedron class is storing a tetrahedron with fields for
 * 			indexes of the corner points and the respective volume. The 
 * 			Tetrahedron class is a tuple and a reductionn variable.
 */
public class TetraClu extends Job{

	
	/* (non-Javadoc)
	 * @see edu.rit.pj2.Job#main(java.lang.String[])
	 * 
	 * main method in accordance with the pj2 library for running the TetraClu program
	 * and finding the tetrahedron with the smallest volume on the given set of cordinates. 
	 * The TatraClu class is run on multiple clusters each having multiple cores using the
	 * pj2 library's masterfor and parallelfor. 
	 * The pj2 library is developer by Professor Alan Kaminsky an is available under the 
	 * GNU General Public License.
	 * 
	 * @param: accepts input file path as a parameters, first arg0[0] is the path for the file which 
	 *  						contains the set of cordinates
	 * 
	 *  @throws Exception 
	 * 				throws Exception when the input is not according to the requirement
	 */
	@Override
	public void main(String[] arg0) throws Exception {
		//decalring the buffered reader for reading the input file
		BufferedReader bufferedReader = null;
		try{
		//to construct an array list to store the cordinates from the input file
		ArrayList<Points> cordList = new ArrayList<Points>();
		//declaring the object for the cordinate class
		Points cord = null;
		String[] tempArray = null;
		//declaring variables for each x, y and z cordinate
		double x,y,z;
		
		//reading the file using buffered reader
		bufferedReader = new BufferedReader(new FileReader(arg0[0]));
		//reading the cordinate line by line
		String readLine = bufferedReader.readLine();
		//read the file until the end
		while(readLine != null){
			//split the temp string read from the input file with regex " "
			tempArray = readLine.split(" ");
			x = Double.parseDouble(tempArray[0]);
			y = Double.parseDouble(tempArray[1]);
			z = Double.parseDouble(tempArray[2]);
			//making the object for storing each cordinate in the arraylist
			cord = new Points(x, y, z);
			//adding the cordinate in the arraylist
			cordList.add(cord);
			//reading the next line from the input file
			readLine = bufferedReader.readLine();
		}
		//declaring x,y and z arrays for each dimensional cordinate
		Points[] cords = new Points[cordList.size()];
		int tempIndex = 0;
		for(Points pointTemp : cordList){
			cords[tempIndex] = pointTemp;
			tempIndex++;
		}
		//putting the tuple in the assigned tuple class to pass the array from the 
		//front end job process to the backend node running the task thread
		
		putTuple(new ObjectArrayTuple<Points>(cords));
		
		//scheduling the masterFor in leapfrog method
		masterSchedule(proportional);
		masterFor (0, cords.length-4, TetraTask.class);
		//calling the multi cluster reduction to reduce the volume on cluster level
		rule() .atFinish() .task (ReduceTetra.class) .runInJobProcess();
		} catch(Exception e){
			//catch the exception when the input command line parameters have not been entered incorrectly
			System.err.println ("Usage: java TetraClu running with pj2. Arguments taken: <inputFile>");
		    System.err.println ("<inputFile> = Path for the input file containing the cordinates");
		    System.err.println ("Remember: path for the input file should be correctly entered.");
			System.err.print("The input file path has been entered incorrectly. Please check and enter correct arguments. Exception: " + e);
		} finally{
			//make sure that the bufferedreader has been closed
			if(bufferedReader != null){
				bufferedReader.close();
			}
		}
	}
	
	/**
	 * @author Utkarsh Bhatia
	 *
	 * Worker Task class running on the backend node
	 */
	private static class TetraTask extends Task{

		//declaring the variables to be used in the main method
		int arrayLength;
		Points[] cords;
		
		//declaring the vbl classs objects for reduction
		Tetrahedron tetraVbl;
		
		/* (non-Javadoc)
		 * @see edu.rit.pj2.Task#main(java.lang.String[])
		 * 
		 * main method called implicitly from the master for method
		 * taking in the command line arguments passed from the job
		 * class in the masterFor method
		 */
		@Override
		public void main(String[] arg0) throws Exception {
			//length passed as the command line argument from the master class
			//length = Integer.parseInt(arg0[0]);
			
			cords = readTuple(new ObjectArrayTuple<Points>()).item;
			arrayLength = cords.length;
			//declare the reduction variable to reduce the volume in a multi-core environment
			tetraVbl = new Tetrahedron();
			//starting the workerfor to run on individual cores
			workerFor().schedule(proportional) .exec(new Loop() {
				//declaring the vbl for multi core reduction
				Tetrahedron reduceVol;
				//declaring the variables to assign values for the four cordinates
				
				/* (non-Javadoc)
				 * @see edu.rit.pj2.LoopBody#start()
				 * Overriding the threads start method
				 */
				@Override
				public void start() throws Exception{
					reduceVol = threadLocal(tetraVbl);
				}
				
				/* (non-Javadoc)
				 * @see edu.rit.pj2.Loop#run(int)
				 * Overriding the run method with arg0 as the index
				 */
				@Override
				public void run(int arg0) throws Exception {
					// TODO Auto-generated method stub
					Tetrahedron tetra = new Tetrahedron();
					for (int i = arg0+1; i < arrayLength-2; i++) {
						for (int j = i+1; j < arrayLength-1; j++) {
							for (int k = j+1; k < arrayLength; k++) {
								tetra.index1 = arg0; tetra.index2=i; tetra.index3=j; tetra.index4=k;
								tetra.calVolume(cords);
								reduceVol.reduce(tetra);
							}
						}
					}
				}
			});
			//making the tuple to put into tuple space for cluster reduction
			putTuple(tetraVbl);
		}
		
	}
	
	/**
	 * @author Utkarsh Bhatia
	 *
	 * Reduction Task Class
	 */
	private static class ReduceTetra extends Task{

		
		/* (non-Javadoc)
		 * @see edu.rit.pj2.Task#main(java.lang.String[])
		 * Reduction task main program
		 */
		public void main(String[] arg0) throws Exception {
			//declaring and assigning the required variables to reduce the 
			//tuple on a cluster level
			Tetrahedron tuple = new Tetrahedron();
			Tetrahedron tetraTuple;
			Tetrahedron reduceTuple = new Tetrahedron();
			int index1, index2, index3, index4;
			index1 = index2 = index3 = index4 = Integer.MIN_VALUE;
						
			//reduce the tuples according to the volume
			while((tetraTuple = tryToTakeTuple(tuple)) != null){
				reduceTuple.reduce(tetraTuple);
			}
			//getting the four indexed from the reduced tuple
			index1 = reduceTuple.getIndex1();
			index2 = reduceTuple.getIndex2();
			index3 = reduceTuple.getIndex3();
			index4 = reduceTuple.getIndex4();
			Points[] cords = readTuple(new ObjectArrayTuple<Points>()).item;
			//calling the method to print the ouptut
			printOutput(cords, reduceTuple.getVolume(), index1, index2, index3, index4);
		}
		
		/**
		 * @param readTup: input tuple to get the cordinates from the given indexes
		 * @param vol: reduced volume
		 * @param index1: first index of the smallest tehrahedron
		 * @param index2: second index of the smallest tehrahedron
		 * @param index3: third index of the smallest tehrahedron
		 * @param index4: fourth index of the smallest tehrahedron
		 */
		private void printOutput(Points[] cords, double vol, int index1, int index2, int index3, int index4){
			
			System.out.print(index1+" (");
			System.out.printf ("%.5g", cords[index1].x);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index1].y);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index1].z);
			System.out.println(")");
			
			System.out.print(index2+" (");
			System.out.printf ("%.5g", cords[index2].x);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index2].y);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index2].z);
			System.out.println(")");
			
			System.out.print(index3+" (");
			System.out.printf ("%.5g", cords[index3].x);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index3].y);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index3].z);
			System.out.println(")");
			
			System.out.print(index4+" (");
			System.out.printf ("%.5g", cords[index4].x);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index4].y);
			System.out.print(",");
			System.out.printf ("%.5g", cords[index4].z);
			System.out.println(")");
			
			System.out.printf ("%.5g", vol);
			System.out.print("\n");
		}
		
	}
	
	/**
	 * @author Utkarsh Bhatia
	 *
	 * Class Tetrahedron provides a reduction variable for the TetraClu and TetraTask shared by
	 * multiple cluster and cores executing a master and worker for.
	 * the class extends Tuple class of pj2 library
	 * the class implement Vbl of pj2 library
	 */
	public static class Tetrahedron extends Tuple implements Vbl{

		//declaring variables to be used in reduction and tuple
		double volume;
		int index1;
		int index2;
		int index3;
		int index4;
		
		/**
		 * declaring the default constructor for the class
		 */
		public Tetrahedron(){
			volume = Double.MAX_VALUE;
			index1 = index2 = index3 = index4 = 0;
		}

		public void calVolume(Points[] cords){
			double x1,x2,x3,x4,y1,y2,y3,y4,z1,z2,z3,z4, localVol = 0;
			x1 = cords[index1].x; x2 = cords[index2].x; x3 = cords[index3].x; x4 = cords[index4].x; 
			y1 = cords[index1].y; y2 = cords[index2].y; y3 = cords[index3].y; y4 = cords[index4].y;
			z1 = cords[index1].z; z2 = cords[index2].z; z3 = cords[index3].z; z4 = cords[index4].z;
			localVol = Math.abs(((
					x1*(y2*(z3 - z4) - y3*(z2 - z4) + y4*(z2 - z3)) - 
					x2*(y1*(z3 - z4) - y3*(z1 - z4) + y4*(z1 - z3)) + 
					x3*(y1*(z2 - z4) - y2*(z1 - z4) + y4*(z1 - z2)) - 
					x4*(y1*(z2 - z3) - y2*(z1 - z3) + y3*(z1 - z2))
					)/6));
			this.volume = localVol;
		}
		
		/* (non-Javadoc)
		 * @see edu.rit.pj2.Vbl#reduce(edu.rit.pj2.Vbl)
		 * 
		 * Reduce the given shared variable into this shared variable. The two
		 * variables are combined together, and the result is stored in this shared
		 * variable. The reduce() method does not need to be multiple
		 * thread safe (thread synchronization is handled by the caller).
		 * @param arg0
		 * 			Shared variable.
		 * returns void
		 * 
		 *   @exception  ClassCastException
		 *     Thrown if the class of arg0 is not
		 *     compatible with the class of this shared variable.
		 */
		@Override
		public void reduce(Vbl arg0) {
			Tetrahedron tetra = (Tetrahedron) arg0;
			if(this.volume > tetra.volume){
				this.set(tetra);
			}
		}

		/**
		 * @param vol
		 * @param ind1
		 * @param ind2
		 * @param ind3
		 * @param ind4
		 */
		public void reduceToSmallest(double vol, int ind1, int ind2, int ind3, int ind4){
			if(this.getVolume() > vol){
				this.setVolume(vol);
				this.setIndex1(ind1);
				this.setIndex2(ind2);
				this.setIndex3(ind3);
				this.setIndex4(ind4);
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.rit.pj2.Vbl#set(edu.rit.pj2.Vbl)
		 * 
		 * Set this shared variable to the given shared variable.
		 *
		 * @param  arg0  Shared variable.
		 */
		@Override
		public void set(Vbl arg0) {
			this.copy((Tetrahedron)arg0);
		}
		
		/* (non-Javadoc)
		 * @see edu.rit.pj2.Tuple#clone()
		 * 
		 * Create a clone of this class
		 * @return Clone.
		 * 
		 * @throws RuntimeException.
		 */
		public Object clone(){
			try{
				Tetrahedron tetra = (Tetrahedron) super.clone();
				tetra.copy(this);
				return tetra;
			}
			catch(Exception e){
				throw new RuntimeException("Shouldn't happen", e);
			}
		}

		/*
		 * @param tetraSmpVbl
		 * Make this Tetrahedron variables be a deep copy 
		 * of the given Tetrahedron variables.
		 * @param Tetrahedron
		 *            Tetrahedron to copy.
		 *            
		 * @return void
		 */
		private void copy(Tetrahedron tetraSmpVbl) {
			// TODO Auto-generated method stub
			this.volume = tetraSmpVbl.volume;
			this.index1 = tetraSmpVbl.index1;
			this.index2 = tetraSmpVbl.index2;
			this.index3 = tetraSmpVbl.index3;
			this.index4 = tetraSmpVbl.index4;
		}

		/**
		 * @return
		 * 		volume
		 */
		public double getVolume() {
			return volume;
		}

		/**
		 * @param volume
		 * 			set the volume entered
		 */
		public void setVolume(double volume) {
			this.volume = volume;
		}

		/**
		 * @return
		 * 		index 1
		 */
		public int getIndex1() {
			return index1;
		}

		/**
		 * @param index1
		 * 			set the index1 entered
		 */
		public void setIndex1(int index1) {
			this.index1 = index1;
		}

		/**
		 * @return
		 * 		index 2
		 */
		public int getIndex2() {
			return index2;
		}

		/**
		 * @param index2
		 * 			set the index2 entered
		 */
		public void setIndex2(int index2) {
			this.index2 = index2;
		}

		/**
		 * @return
		 * 			index 3
		 */
		public int getIndex3() {
			return index3;
		}

		/**
		 * @param index3
		 * 			set the index 3 entered
		 */
		public void setIndex3(int index3) {
			this.index3 = index3;
		}

		/**
		 * @return
		 * 		index4
		 */
		public int getIndex4() {
			return index4;
		}

		/**
		 * @param index4
		 * 			set the index 4 entered
		 */
		public void setIndex4(int index4) {
			this.index4 = index4;
		}

		/* (non-Javadoc)
		 * @see edu.rit.pj2.Tuple#readIn(edu.rit.io.InStream)
		 * 
		 * to read the tuple to the output when the read tuple is required
		 */
		@Override
		public void readIn(InStream arg0) throws IOException {
			volume = arg0.readDouble();
			index1 = arg0.readInt();
			index2 = arg0.readInt();
			index3 = arg0.readInt();
			index4 = arg0.readInt();
		}

		/* (non-Javadoc)
		 * @see edu.rit.pj2.Tuple#writeOut(edu.rit.io.OutStream)
		 * 
		 * to write to the tuple when the put tuple has been called
		 */
		@Override
		public void writeOut(OutStream arg0) throws IOException {
			arg0.writeDouble(volume);
			arg0.writeInt(index1);
			arg0.writeInt(index2);
			arg0.writeInt(index3);
			arg0.writeInt(index4);
		}
	}
}

/**
 * @author Utkarsh Bhatia
 *
 */
class Points implements Streamable{
	//declaring individual x,y and zcordinates for a 3 dimensional input cordinate
	double x;
	double y;
	double z;
	
	public Points(){
		
	}
	
	/**
	 * @param x: input x cordinate
	 * @param y: input y cordinate
	 * @param z: input z cordinate
	 */
	public Points(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void readIn(InStream arg0) throws IOException {
		// TODO Auto-generated method stub
		x = (double) arg0.readDouble();
		y = (double) arg0.readDouble();
		z = (double) arg0.readDouble();
	}

	@Override
	public void writeOut(OutStream arg0) throws IOException {
		// TODO Auto-generated method stub
		arg0.writeDouble(x);
		arg0.writeDouble(y);
		arg0.writeDouble(z);
	}
}