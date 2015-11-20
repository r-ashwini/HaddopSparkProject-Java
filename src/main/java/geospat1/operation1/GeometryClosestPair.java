package geospat1.operation1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.broadcast.Broadcast;

public class GeometryClosestPair {
    public static boolean flag;
    
    public static void main(String[] args) throws IOException {
		System.out.println(GeometryClosestPair.Closest(args[0],args[1]));
				
	}

	public static class Pair
	  {
	    public Point point1 = null;
	    public Point point2 = null;
	    List<Point> subsidaryPoints = new ArrayList<Point>();
	    public double distance = 0.0;
	 
	    public void updateAdditional(List<Point> subsidaryPoints)
	    {  
	    	this.subsidaryPoints = subsidaryPoints;
	    }
	 
	    public Pair(Point point1, Point point2)
	    {
	      this.point1 = point1;
	      this.point2 = point2;
	      calcDistance();
	    }
	 
	    public void update(Point point1, Point point2, double distance)
	    {
	      this.point1 = point1;
	      this.point2 = point2;
	      this.distance = distance;
	    }
	 
	    public void calcDistance()
	    {  this.distance = distance(point1, point2);  }
	 
	    public String toString()
	    {  return point1 + "-" + point2 + " : " + distance;  }
	  }
	 
	  public static double distance(Point p1, Point p2)
	  {
	    double xdist = p2.getX() - p1.getX();
	    double ydist = p2.getY() - p1.getY();
	    return Math.hypot(xdist, ydist);
	  }
	 
	  public static Pair bruteForce(List<? extends Point> points)
	  {
	    int numPoints = points.size();
	    if (numPoints < 2)
	      return null;
	    Pair pair = new Pair(points.get(0), points.get(1));
	    if (numPoints > 2)
	    {
	      for (int i = 0; i < numPoints - 1; i++)
	      {
	        Point point1 = points.get(i);
	        for (int j = i + 1; j < numPoints; j++)
	        {
	          Point point2 = points.get(j);
	          double distance = distance(point1, point2);
	          if (distance < pair.distance)
	            pair.update(point1, point2, distance);
	        }
	      }
	    }
	    return pair;
	  }
	  public static void sortByX(List<? extends Point> points)
	  {
	    Collections.sort(points, new Comparator<Point>() {
	        public int compare(Point point1, Point point2)
	        {
	          if (point1.getX() < point2.getX())
	            return -1;
	          if (point1.getX() > point2.getX())
	            return 1;
	          return 0;
	        }
	      }
	    );
	  }
	  public static Pair divideAndConquer(List<? extends Point> points)
	  {
	    List<Point> pointsSortedByX = new ArrayList<Point>(points);
	    sortByX(pointsSortedByX);
	    List<Point> pointsSortedByY = new ArrayList<Point>(points);
	    sortByY(pointsSortedByY);
	    return divideAndConquer(pointsSortedByX, pointsSortedByY);
	  }
	  private static Pair divideAndConquer(List<? extends Point> pointsSortedByX, List<? extends Point> pointsSortedByY)
	  {
	    int numPoints = pointsSortedByX.size();
	    if (numPoints <= 3)
	      return bruteForce(pointsSortedByX);
	 
	    int dividingIndex = numPoints >>> 1;
	    List<? extends Point> leftOfCenter = pointsSortedByX.subList(0, dividingIndex);
	    List<? extends Point> rightOfCenter = pointsSortedByX.subList(dividingIndex, numPoints);
	 
	    List<Point> tempList = new ArrayList<Point>(leftOfCenter);
	    sortByY(tempList);
	    Pair closestPair = divideAndConquer(leftOfCenter, tempList);
	 
	    tempList.clear();
	    tempList.addAll(rightOfCenter);
	    sortByY(tempList);
	    Pair closestPairRight = divideAndConquer(rightOfCenter, tempList);
	 
	    if (closestPairRight.distance < closestPair.distance)
	      closestPair = closestPairRight;
	 
	    tempList.clear();
	    double shortestDistance =closestPair.distance;
	    double centerX = rightOfCenter.get(0).getX();
	    for (Point point : pointsSortedByY)
	      if (Math.abs(centerX - point.getX()) < shortestDistance)
	        tempList.add(point);
	 
	    for (int i = 0; i < tempList.size() - 1; i++)
	    {
	      Point point1 = tempList.get(i);
	      for (int j = i + 1; j < tempList.size(); j++)
	      {
	        Point point2 = tempList.get(j);
	        if ((point2.getY() - point1.getY()) >= shortestDistance)
	          break;
	        double distance = distance(point1, point2);
	        if (distance < closestPair.distance)
	        {
	          closestPair.update(point1, point2, distance);
	          shortestDistance = distance;
	        }
	      }
	    }
	    
	    return closestPair;
	  }
	 
	  public static void sortByY(List<? extends Point> points)
	  {
	    Collections.sort(points, new Comparator<Point>() {
	        public int compare(Point point1, Point point2)
	        {
	          if (point1.getY() < point2.getY())
	            return -1;
	          if (point1.getY() > point2.getY())
	            return 1;
	          return 0;
	        }
	      }
	    );
	  }
	  
	 /* def this(clientArgs: ClientArguments, spConf: SparkConf) =
			    this(clientArgs, new Configuration(), spConf)*/

		public static int count=0;	

	public static Boolean Closest(String input,String output) {


		SparkConf conf=new SparkConf().setAppName("ClosestPair").setMaster("local");
		JavaSparkContext sc=new JavaSparkContext(conf); 
	   	    String  x = input.trim();
	    		//"/home/system/Downloads/ProjectTestCase/FarthestPairandClosestPairTestData.csv";
	    Broadcast<String> path = sc.broadcast(x);
	    JavaRDD<String> str1=sc.textFile(path.value());
	   //Find the points of polygon and load into RDD
	    /*Following is the RDD : "helo" is  for small dataset.
	     *  
	     * If you are using big dataset, comment the below RDD and uncomment next part. Instructions below
	     * 
	     * */
	    
	  /*  JavaRDD<String> helo = str1.map(new Function<String,String> ()
	            {
	                public String call(String s)
	                {
	                	 double num[]=new double[2];
	                   
	                    int i=0;
	                    if(s.equalsIgnoreCase("x,y")){return "";}
	                    System.out.println(s);
	                    String[] d=s.split(",");
	                    for(int j=0;j<d.length;j++){
	                    	num[j]=Double.parseDouble(d[j].trim());
	                    }
	                    
	                  
	                    
	                    String u=Arrays.toString(num);
	                    return u; 
	                }
	            });*/
	    
	    // TODO if the data set if big comment the above RDD named helo and uncomment the following two RDD's named : helo1,helo2
	  
	    JavaRDD<String> helo1 = str1.map(new Function<String,String> ()
	            {
	                public String call(String s)
	                {
	                	 double num[]=new double[2];
	                   
	                    int i=0;
	                    String[] d=s.split(",");
	                    String[] new1 = new String[2];
	                   
	                    new1[0]=d[2];
	                    new1[1]=d[3];
	                    
	                    for(int j=0;j<new1.length;j++){
	                    	num[j]=Double.parseDouble(new1[j].trim());
	                    }
	                    String u=Arrays.toString(num);
	                    return u; 
	                }
	            });
	    JavaRDD<String> helo2 = str1.map(new Function<String,String> ()
	            {
	                public String call(String s)
	                {
	                	 double num[]=new double[2];
	                   
	                    int i=0;
	                    String[] d=s.split(",");
	                    String[] new1 = new String[2];
	                    new1[0]=d[4];
	                    new1[1]=d[5];
	                    
	                    for(int j=0;j<new1.length;j++){
	                    	num[j]=Double.parseDouble(new1[j].trim());
	                    }
	                    String u=Arrays.toString(num);
	                    return u; 
	                }
	            });
	   
	    JavaRDD<String> helo = helo1.union(helo2);
	  
	     
	    // TODO end of BIG dataset commenting area
	    
	    JavaRDD<Pair> ClosestPair = helo.mapPartitions(new FlatMapFunction<Iterator<String>,Pair> (){

			@Override
			public Iterable<Pair> call(Iterator<String> t) throws Exception {
                List<Point> points = new ArrayList<Point>();
                ArrayList<String> x = new ArrayList<String>();
                ArrayList<Integer> y = new ArrayList<Integer>();
                List<Pair> result = new ArrayList<Pair>();
                count++;
                int j = 0;
                while (t.hasNext()) {
                	String string = t.next();
                	if(string.equalsIgnoreCase("")){if(t.hasNext()==true){string=t.next();} else{break;}}
                x.add(string);
                String p2 = x.get(j++);
                p2 = p2.substring(1, (p2.length() - 1)).replaceAll(" ","");
                String[] p1 = p2.split(",");
                double num[] = new double[p1.length];
                for (int i = 0; i < p1.length; i++) {

                num[i] = Double.valueOf(p1[i]);
                num[i+1] = Double.valueOf(p1[i+1]);
                points.add(new Point(num[i], num[i+1]));
                
                i++;
                }
                }
                System.out.println("\n\n------------");
                for(Point p: points)
                {
                	System.out.println(p.getX()+","+p.getY());
                }
                flag = true;
                Pair dqClosestPair = divideAndConquer(points);
                if(flag==true){
                	sortByX(points);
            	    double cornerDistLeft=points.get(0).getX()+dqClosestPair.distance;
            	    double cornerDistRight= points.get(points.size()-1).getX() - dqClosestPair.distance;
            	    List<Point> extraPoints = new ArrayList<Point>();
            	    List<Point> Temporary = new ArrayList<Point>(points);
            	    sortByX(Temporary);
            	    extraPoints.add(dqClosestPair.point1);
            	    extraPoints.add(dqClosestPair.point2);

            	    for(Point p:Temporary)
            	    {
            	    	if(p.getX()<=cornerDistLeft)
            	    	{
            	    		if(extraPoints.contains(p)==false){
            	    		extraPoints.add(p);}
            	    	}
            	    	else if(p.getX()>cornerDistLeft)
            	    	{
            	    		break;
            	    	}
            	    }
            	   
            	    for(int m=Temporary.size()-1;m>=0;m--)
            	    {
            	    	if(Temporary.get(m).getX()>=cornerDistRight)
            	    	{
            	    		if(extraPoints.contains(Temporary.get(m))==false){
            	    		extraPoints.add(Temporary.get(m));}
            	    	}
            	    	else if(Temporary.get(m).getX()<cornerDistRight)
            	    	{
            	    		break;
            	    	}
            	    }
            	   dqClosestPair.updateAdditional(extraPoints);
            	    
            	   
            	   }
                System.out.println(dqClosestPair.point1+","+dqClosestPair.point2+","+dqClosestPair.distance+" List of points: "+dqClosestPair.subsidaryPoints);
                

                result.add(dqClosestPair);
                return result;
			}
	    	
	    });
	    
	    JavaRDD<Pair> finalResult = ClosestPair.coalesce(1);
	    
	    JavaRDD<Pair> FinalClosestPair = finalResult.mapPartitions(new FlatMapFunction<Iterator<Pair>,Pair> (){

			@Override
			public Iterable<Pair> call(Iterator<Pair> t) throws Exception {
                List<Point> points = new ArrayList<Point>();
                ArrayList<String> x = new ArrayList<String>();
                ArrayList<Integer> y = new ArrayList<Integer>();
                List<Pair> result = new ArrayList<Pair>();


                int j = 0;
                while (t.hasNext()) {
                Pair m = t.next();

                for(Point l:m.subsidaryPoints)
                {
                	 String p2 = l.getX()+","+l.getY();
                     String[] p1 = p2.split(",");
                     double num[] = new double[p1.length];
                     for (int i = 0; i < p1.length; i++) {

                     num[i] = Double.valueOf(p1[i]);
                     num[i+1] = Double.valueOf(p1[i+1]);
                     points.add(new Point(num[i], num[i+1]));
                     
                     i++;
                     }
                }

               
                }
                System.out.println("\n\n------------");
                for(Point p: points)
                {
                	System.out.println(p.getX()+","+p.getY());
                }
                flag = false;
                Pair dqClosestPair = divideAndConquer(points);
                System.out.println(dqClosestPair.point1+","+dqClosestPair.point2+","+dqClosestPair.distance+" List of extra points: "+dqClosestPair.subsidaryPoints);
                result.add(dqClosestPair);
                return result;
			}
	    	
	    });
	    FinalClosestPair.foreach(new VoidFunction<Pair>(){

			@Override
			public void call(Pair t) throws Exception {
				// TODO Auto-generated method stub
				System.out.println(t.point1+","+t.point2);
			}});
	    boolean flag = true;
	    try{
	    FinalClosestPair.saveAsTextFile(output.trim());
	    }
	    catch(Exception e)
	    {
	    	flag = false;
	     System.out.println(e.getMessage());
	    }
	    finally
	    {
	    	return flag;
	    }
	}//end of Closest
	
}//end of class
    

