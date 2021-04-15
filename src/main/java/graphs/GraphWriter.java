package graphs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

public class GraphWriter {


	static FileWriter fileWriterNodes;
	static FileWriter fileWriterConnections;

	public static void main(String[] args) throws IOException, InterruptedException {

		File file = new File("src\\main\\java\\graphs\\simple-harbour.dot");
		if (file.exists())
			file.delete();

		File file1 = new File("src\\main\\java\\graphs\\simple-harbour-nodes.dot");
		if (file1.exists())
			file1.delete();
		fileWriterNodes = new FileWriter("src\\main\\java\\graphs\\simple-harbour-nodes.dot", true);

		File file2 = new File("src\\main\\java\\graphs\\simple-harbour-connections.dot");
		if (file2.exists())
			file2.delete();
		fileWriterConnections = new FileWriter("src\\main\\java\\graphs\\simple-harbour-connections.dot", true);

		fileWriterNodes.append("digraph mapgraph { \n");

		roadBlock(20, 20, 20 + 200, 14+100, true, 10);
		// dubbelRoad(4, 4, 6, 20, true);
		
		dubbelRoad(44, 116, 190, 118, false, true, 20);
		
		//TO return to previous delete evert last to and uncomment in roadblock 
		/*
		fileWriterConnections.append("nx" + 20 + "y" + 86 + " -> nx" + 20 + "y" + 88 + "[d=20]\n");
		fileWriterConnections.append("nx" + 22 + "y" + 88 + " -> nx" + 22 + "y" + 86 + "[d=20]\n");
		fileWriterConnections.append("nx" + 20 + "y" + 88 + " -> nx" + 20 + "y" + 90 + "[d=20]\n");
		fileWriterConnections.append("nx" + 22 + "y" + 90 + " -> nx" + 22 + "y" + 88 + "[d=20]\n");
		*/
		
		fileWriterConnections.append("nx" + 44 + "y" + 114 + " -> nx" + 44 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 46 + "y" + 116 + " -> nx" + 46 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 44 + "y" + 116 + " -> nx" + 44 + "y" + 118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 46 + "y" + 118 + " -> nx" + 46 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 68 + "y" + 114 + " -> nx" + 68 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 70 + "y" + 116 + " -> nx" + 70 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 68 + "y" + 116+ " -> nx" + 68 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 70 + "y" + 118 + " -> nx" + 70 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 92 + "y" + 114 + " -> nx" + 92 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 94 + "y" + 116 + " -> nx" + 94 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 92 + "y" + 116 + " -> nx" + 92 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 94 + "y" + 118 + " -> nx" + 94 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 116 + "y" + 114 + " -> nx" + 116 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 118 + "y" + 116 + " -> nx" + 118 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 116 + "y" + 116+ " -> nx" + 116 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 118 + "y" + 118 + " -> nx" + 118 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 140 + "y" + 114 + " -> nx" + 140 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 142 + "y" + 116 + " -> nx" + 142 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 140 + "y" + 116 + " -> nx" + 140 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 142 + "y" + 118 + " -> nx" + 142 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 164 + "y" + 114 + " -> nx" + 164 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 166 + "y" + 116 + " -> nx" + 166 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 164 + "y" + 116 + " -> nx" + 164 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 166 + "y" + 118 + " -> nx" + 166 + "y" + 116 + "[d=20]\n");
		
		fileWriterConnections.append("nx" + 188 + "y" + 114 + " -> nx" + 188 + "y" + 116 + "[d=20]\n");
		fileWriterConnections.append("nx" + 190 + "y" + 116 + " -> nx" + 190 + "y" + 114 + "[d=20]\n");
		fileWriterConnections.append("nx" + 188 + "y" + 116 + " -> nx" + 188 + "y" +  118 + "[d=20]\n");
		fileWriterConnections.append("nx" + 190 + "y" + 118 + " -> nx" + 190 + "y" + 116 + "[d=20]\n");
		
		
		//lineSAHor(24, 92,42, "OUT", true);
		lineSAHor(48, 120,66, "OUT", true);
		lineSAHor(72, 120,90, "OUT", true);
		lineSAHor(96, 120,114, "OUT", true);
		lineSAHor(120, 120, 138 , "OUT", true);
		lineSAHor(144, 120, 162 , "OUT", true);
		lineSAHor(168, 120, 186 , "OUT", true);
		

		lineSAHor(48, 18,66, "IN", false);
		lineSAHor(72, 18,90, "IN", false);
		lineSAHor(96, 18,114, "IN", false);
		lineSAHor(120, 18, 138 , "IN", false);
		lineSAHor(144, 18, 162 , "IN", false);
		lineSAHor(168, 18, 186 , "IN", false);
		
		//lineSA(18, 30, 52, true, "IN");
		//lineSA(96, 30, 52, false, "OUT");

		System.out.println("Writing Nodes");
		fileWriterConnections.append("}\n");

		fileWriterNodes.flush();
		fileWriterNodes.close();

		TimeUnit.SECONDS.sleep(1);
		System.out.println("Writing Connections");
		fileWriterConnections.flush();
		fileWriterConnections.close();
		
		File[] files = new File[3];
		files[0] = file1;
		files[1] = new File("src\\main\\java\\graphs\\simple-harbour-ChargingStations.dot");
		files[2] = file2;

		joinFiles(file, files);
		
		System.out.println("Finished Writing");
	}

	@SuppressWarnings("unused")
	private static void lineSA(int startX, int startY, int endY, boolean left, String type) throws IOException {
		for (int y = startY; y < endY; y += 2) {
			fileWriterNodes.append("nx" + startX + "y" + y + "[p=\"" + startX + "," + y + "\", ammount=\"12\", type=\" "
					+ type + "\"]\n");
			int temp;
			if (left)
				temp = startX + 2;
			else
				temp = startX - 2;

			fileWriterConnections.append("nx" + startX + "y" + y + " -> nx" + temp + "y" + y + "[d=20]\n");
			fileWriterConnections.append("nx" + temp + "y" + y + " -> nx" + startX + "y" + y + "[d=20]\n");
		}
	}
	
	private static void lineSAHor(int startX, int startY, int endX, String type, boolean UP) throws IOException {
		for (int x = startX; x < endX; x += 4) {
			fileWriterNodes.append("nx" + x + "y" + startY + "[p=\"" + x + "," + startY + "\", ammount=\"12\", type=\" "
					+ type + "\"]\n");

			int temp;
			if (UP)
				temp = startY - 2;
			else
				temp = startY +2;
			
			fileWriterConnections.append("nx" + x + "y" + startY + " -> nx" + x + "y" + temp + "[d=20]\n");
			fileWriterConnections.append("nx" + x + "y" + temp + " -> nx" + x + "y" + startY + "[d=20]\n");
		}
	}

	/**
	 * 
	 * @param startx
	 * @param starty
	 * @param endX
	 * @param endY
	 * @param vertical
	 * @param widthRow aantal stations tussen de 2 hoofdwegen
	 * @throws IOException
	 */
	public static void roadBlock(int startx, int starty, int endX, int endY, boolean vertical, int widthRow)
			throws IOException {
		if (vertical) {
			int x = startx;
			while (x <= endX) {
				// makes the vertical roads
				dubbelRoad(x, starty, x + 2, endY, vertical, false, 20);
				// makes the horizontal roads
				// and couples them with the vertical roads
				if (x + 4 + widthRow * 2 <= endX) {
					int loopy = starty;
					do {
						connectingRoad(x, loopy, widthRow, vertical);
						loopy += 10;
					} while (loopy < endY);

					// Here the SA should be implemented
					loopy = starty;
					while (loopy + 10 < endY) {
						generateDubbelSANodes(loopy + 4, loopy + 8, x + 4, x + widthRow * 2 + 2);
						loopy += 10;
					}
					if (loopy + 4 <= endY) {
						generateSingleSANodes(loopy + 4, x + 4, x + widthRow * 2 + 2);
					}
				}
				x = x + 4 + widthRow * 2;
			}
		} else {
			System.err.println("Horizontal block are not yet implemented");
		}
	}

	private static void generateSingleSANodes(int y1, int startx, int endx) throws IOException {
		int temp = y1 - 2;
		for (int x = startx+2; x < endx; x += 2) {
			fileWriterNodes.append("nx" + x + "y" + y1 + "[p=\"" + x + "," + y1 + "\", ammount=\"12\", type=\"SA\"]\n");

			fileWriterConnections.append("nx" + x + "y" + y1 + " -> nx" + x + "y" + temp + "[d=20]\n");
			fileWriterConnections.append("nx" + x + "y" + temp + " -> nx" + x + "y" + y1 + "[d=20]\n");
		}

		fileWriterNodes.append("nx" + endx + "y" + y1 + "[p=\"" + endx + "," + y1 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + endx + "y" + y1 + " -> nx" + endx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + endx + "y" + temp + " -> nx" + endx + "y" + y1 + "[d=20]\n");
		fileWriterNodes.append("nx" + startx + "y" + y1 + "[p=\"" + startx + "," + y1 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + startx + "y" + y1 + " -> nx" + startx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + startx + "y" + temp + " -> nx" + startx + "y" + y1 + "[d=20]\n");
	}

	private static void generateDubbelSANodes(int y1, int y2, int startx, int endx) throws IOException {
		for (int x = startx +2; x < endx; x += 2) {
			fileWriterNodes.append("nx" + x + "y" + y1 + "[p=\"" + x + "," + y1
					+ "\", ammount=\"12\", type=\"SAC\" , connected=\" " + x + "," + y2 + "\"]\n");
			fileWriterNodes.append("nx" + x + "y" + y2 + "[p=\"" + x + "," + y2
					+ "\", ammount=\"12\", type=\"SAC\" , connected=\" " + x + "," + y1 + "\"]\n");

			int temp = y1 - 2;
			fileWriterConnections.append("nx" + x + "y" + y1 + " -> nx" + x + "y" + temp + "[d=20]\n");
			fileWriterConnections.append("nx" + x + "y" + temp + " -> nx" + x + "y" + y1 + "[d=20]\n");

			temp = y2 + 2;
			fileWriterConnections.append("nx" + x + "y" + y2 + " -> nx" + x + "y" + temp + "[d=20]\n");
			fileWriterConnections.append("nx" + x + "y" + temp + " -> nx" + x + "y" + y2 + "[d=20]\n");
		}

		int temp = y1 - 2;
		fileWriterNodes.append("nx" + endx + "y" + y1 + "[p=\"" + endx + "," + y1 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + endx + "y" + y1 + " -> nx" + endx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + endx + "y" + temp + " -> nx" + endx + "y" + y1 + "[d=20]\n");
		fileWriterNodes.append("nx" + startx + "y" + y1 + "[p=\"" + startx + "," + y1 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + startx + "y" + y1 + " -> nx" + startx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + startx + "y" + temp + " -> nx" + startx + "y" + y1 + "[d=20]\n");

		temp = y2 + 2;
		fileWriterNodes.append("nx" + endx + "y" + y2 + "[p=\"" + endx + "," + y2 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + endx + "y" + y2 + " -> nx" + endx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + endx + "y" + temp + " -> nx" + endx + "y" + y2 + "[d=20]\n");
		fileWriterNodes.append("nx" + startx + "y" + y2 + "[p=\"" + startx + "," + y2 + "\", type=\"RP\"]\n");
		fileWriterConnections.append("nx" + startx + "y" + y2 + " -> nx" + startx + "y" + temp + "[d=20]\n");
		fileWriterConnections.append("nx" + startx + "y" + temp + " -> nx" + startx + "y" + y2 + "[d=20]\n");
	}

	public static void connectingRoad(int x, int y, int widthRow, boolean vertical) throws IOException {
		int hx = x + 4;
		int hy = y;
		dubbelRoad(hx, hy, hx + widthRow * 2 - 2, hy + 2, !vertical, true, 20);

		// adding connections to nodes that are missing
		int tempx = hx - 2;
		int temx2 = hx;
		int tempy2 = hy + 2;
		fileWriterConnections.append("nx" + temx2 + "y" + hy + " -> nx" + tempx + "y" + hy + "[d=20]\n");
		fileWriterConnections.append("nx" + tempx + "y" + tempy2 + " -> nx" + hx + "y" + tempy2 + "[d=20]\n");

		tempx = hx + widthRow * 2 - 2;
		temx2 = hx + widthRow * 2;
		tempy2 = hy + 2;
		fileWriterConnections.append("nx" + temx2 + "y" + hy + " -> nx" + tempx + "y" + hy + "[d=20]\n");
		fileWriterConnections.append("nx" + tempx + "y" + tempy2 + " -> nx" + temx2 + "y" + tempy2 + "[d=20]\n");

		// fileWriterConnections.flush();
	}

	public static void dubbelRoad(int startx, int starty, int endX, int endY, boolean vertical, boolean holdIA,
			int distance) throws IOException {
		String type = "";
		if (holdIA) {
			type = ", type = HoldIA";
		}
		for (int x = startx; x <= endX; x += 2) {
			for (int y = starty; y <= endY; y += 2) {
				fileWriterNodes.append("nx" + x + "y" + y + "[p=\"" + x + "," + y + "\" " + type + "]\n");
			}

		}
		// fileWriterNodes.flush();

		if (vertical) {
			int x = startx;
			for (int y = starty; y <= endY - 2; y += 2) {
				int nexty = y + 2;
				fileWriterConnections
						.append("nx" + x + "y" + y + " -> " + "nx" + x + "y" + nexty + "[d=" + distance + "]\n");
			}
			x = startx + 2;
			for (int y = starty; y <= endY - 2; y += 2) {
				int nexty = y + 2;
				fileWriterConnections
						.append("nx" + x + "y" + nexty + " -> nx" + x + "y" + y + "[d=" + distance + "]\n");
			}

			for (int y = starty; y <= endY; y += 2) {
				x = startx;
				int nextx = startx + 2;
				fileWriterConnections.append("nx" + x + "y" + y + " -> nx" + nextx + "y" + y + "[d= 20 ]\n");
				fileWriterConnections.append("nx" + nextx + "y" + y + " -> nx" + x + "y" + y + "[d= 20 ]\n");
			}

		} else {
			int y = starty;
			for (int x = startx; x <= endX - 2; x += 2) {
				int nextx = x + 2;
				fileWriterConnections
						.append("nx" + nextx + "y" + y + " -> nx" + x + "y" + y + "[d=" + distance + "]\n");

			}
			y = starty + 2;
			for (int x = startx; x <= endX - 2; x += 2) {
				int nextx = x + 2;
				fileWriterConnections
						.append("nx" + x + "y" + y + " -> " + "nx" + nextx + "y" + y + "[d=" + distance + "]\n");
			}

			/*
			for (int x = startx; x <= endX; x += 2) {
				y = starty;
				int nexty = starty + 2;
				fileWriterConnections.append("nx" + x + "y" + y + " -> nx" + x + "y" + nexty + "[d= 20 ]\n");
				fileWriterConnections.append("nx" + x + "y" + nexty + " -> nx" + x + "y" + y + "[d= 20 ]\n");
			}
			*/

		}
	}

	public static void joinFiles(File destination, File[] sources) throws IOException {
		OutputStream output = null;
		try {
			output = createAppendableStream(destination);
			for (File source : sources) {
				appendFile(output, source);
			}
		} finally {
			IOUtils.closeQuietly(output);
		}
	}

	private static BufferedOutputStream createAppendableStream(File destination) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(destination, true));
	}

	private static void appendFile(OutputStream output, File source) throws IOException {
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			IOUtils.copy(input, output);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
}
