package com.difegue.doujinsoft;

import com.difegue.doujinsoft.utils.MioCompress;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.Kernel;
import java.awt.image.ConvolveOp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DownloadServlet
 */
@WebServlet("/download")
public class DownloadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DownloadServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 *      Returns .mio files from the dataDirectory so they can be downloaded, or
	 *      their image previews.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// obtains ServletContext
		ServletContext application = getServletConfig().getServletContext();
		String dataDir = application.getInitParameter("dataDirectory");

		if (request.getParameterMap().containsKey("type") && request.getParameterMap().containsKey("id")) {

			String id = request.getParameter("id");
			String type = request.getParameter("type");
			boolean isImageOnly = request.getParameterMap().containsKey("preview");

			boolean isGame = type.equals("game");
			boolean isRecord = type.equals("record");
			boolean isManga = type.equals("manga");

			// Only serve an image if that's what's asked
			if (isImageOnly && (isGame || isManga)) {

				String statement = isGame ? "SELECT previewPic, isNsfw FROM Games WHERE hash == ?"
						: "SELECT frame0 FROM Manga WHERE hash == ?";

				try (Connection connection = DriverManager
						.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

					PreparedStatement ret = connection.prepareStatement(statement);
					ret.setString(1, id);
					ResultSet result = ret.executeQuery();

					String base64ImageData = result.getString(1).replace("data:image/png;base64,", "");
					byte[] imageData = Base64.getDecoder().decode(base64ImageData);

					if (isGame) {
						boolean isNsfw = result.getBoolean(2);
						if (isNsfw) {

							// Blur most of the preview image
							int radius = 5;
							int size = radius * 2 + 1;
							float weight = 1.0f / (size * size);
							float[] data = new float[size * size];

							for (int i = 0; i < data.length; i++) {
								data[i] = weight;
							}

							Kernel kernel = new Kernel(size, size, data);
							ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
							BufferedImage blurredImage = op.filter(ImageIO.read(new ByteArrayInputStream(imageData)),
									null);

							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(blurredImage, "png", baos);
							imageData = baos.toByteArray();
						}
					}

					response.setContentType("image/png");
					response.setCharacterEncoding("UTF-8");

					OutputStream outStream = response.getOutputStream();
					outStream.write(imageData);
					outStream.flush();
					outStream.close();
					return;

				} catch (Exception e) {
					response.setContentType("image/jpg");
					response.setCharacterEncoding("UTF-8");

					application.getResourceAsStream("/img/meta.jpg").transferTo(response.getOutputStream());
					return;
				}
			}

			// Record or failed game/manga fallback preview picture
			if (isImageOnly) {

				response.setContentType("image/jpg");
				response.setCharacterEncoding("UTF-8");

				application.getResourceAsStream("/meta.jpg").transferTo(response.getOutputStream());
				return;
			}

			String filePath = null;

			if (isGame)
				filePath = dataDir + "/mio/game/" + id + ".miozip";
			else if (isRecord)
				filePath = dataDir + "/mio/record/" + id + ".miozip";
			else if (isManga)
				filePath = dataDir + "/mio/manga/" + id + ".miozip";

			if (filePath != null) {

				File downloadFile = MioCompress.uncompressMio(new File(filePath));
				FileInputStream inStream = new FileInputStream(downloadFile);

				// gets MIME type of the file
				String mimeType = "application/octet-stream";
				// Set response
				response.setContentType(mimeType);
				response.setCharacterEncoding("UTF-8");
				response.setContentLength((int) downloadFile.length());

				// forces download
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename*=UTF-8''%s",
						URLEncoder.encode(downloadFile.getName(), "UTF-8"));
				response.setHeader(headerKey, headerValue);

				// obtains response's output stream
				OutputStream outStream = response.getOutputStream();

				byte[] buffer = new byte[4096];
				int bytesRead = -1;

				while ((bytesRead = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}

				inStream.close();
				outStream.close();
			}

		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
