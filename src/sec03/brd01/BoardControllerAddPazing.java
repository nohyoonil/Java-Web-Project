package sec03.brd01;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;



@WebServlet("/board/*")//브라우저에서 요칭 시 두 단계로 요청이 이루어짐
public class BoardControllerAddPazing extends HttpServlet {
	private static String ARTICLE_IMAGE_REPO = "C:\\board\\article_image";//글에 첨부한 이미지 저장 위치를 상수로 선언
	BoardService boardService;
	ArticleVO articleVO;
	HttpSession session;//답글에 대한 부모 글 번호를 저장하기 위해 세션을 사용

	public void init(ServletConfig config) throws ServletException {
		boardService = new BoardService();//서블릿 초기화 시 BoardService 객체를 생성
		articleVO = new ArticleVO();  
	}
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doHandle(request, response);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doHandle(request, response);
	}
	private void doHandle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html;charset=utf-8");
		String nextPage=null;
		String action=request.getPathInfo();
		System.out.println("action: " + action);
		
		try {
			List<ArticleVO> articlesList = new ArrayList<ArticleVO>();
			if(action == null || action.equals("/listArticles.do")) {
				String _section = request.getParameter("section");//
				String _pageNum = request.getParameter("pageNum");//최초 요청 시 또는 /listArticles.do로 요청 시 section, pageNum 값을 구함
				int section = Integer.parseInt(((_section == null)? "1" : _section));//
				int pageNum = Integer.parseInt(((_pageNum == null)? "1" : _pageNum));//최초 요청 시 section, pageNum 값이 없으면 각각 1로 초기화
				Map<String, Integer> pagingMap = new HashMap<String, Integer>();
				pagingMap.put("section", section);
				pagingMap.put("pageNum", pageNum);
				Map articlesMap = boardService.listArticles(pagingMap);//section 값과 pageNum 값으로 해당 섹션과 페이지에 해당되는 글 목록 조회
				articlesMap.put("section", section);
				articlesMap.put("pageNum", pageNum);
				request.setAttribute("articlesMap", articlesMap);//조회된 글 목록을 articlesMap으로 바인딩하여 listArticles.jsp로 넘김
				nextPage="/board01/listArticles.jsp";
				
			}else if(action.equals("/articleForm.do")) {
				nextPage="/board01/articleForm.jsp";
				
			}else if(action.equals("/addArticle.do")) {
				int articleNO = 0;
				Map<String, String> articleMap = upload(request, response);
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName");
				
				articleVO.setParentNO(0);
				articleVO.setId("hong");
				articleVO.setTitle(title);
				articleVO.setContent(content);
				articleVO.setImageFileName(imageFileName);
				articleNO = boardService.addArticle(articleVO); 
				
				if(imageFileName !=null && imageFileName.length() !=0) {
					File srcFile = new File(ARTICLE_IMAGE_REPO+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(ARTICLE_IMAGE_REPO+"\\"+articleNO);
					destDir.mkdir();
					FileUtils.moveFileToDirectory(srcFile, destDir, true);
					srcFile.delete();
				}
				PrintWriter pw = response.getWriter();
				pw.print("<script> alert('새글을 추가했습니다.'); location.href='" + 
							request.getContextPath() + "/board/listArticles.do';" + "</script>");
				return;
				
			}else if(action.equals("/viewArticle.do")) {
				String articleNO = request.getParameter("articleNO");
				articleVO = boardService.viewArticle(Integer.parseInt(articleNO));
				request.setAttribute("article", articleVO);
				nextPage = "/board01/viewArticle.jsp";
				
			}else if(action.equals("/modArticle.do")) {
				Map<String, String> articleMap = upload(request, response);
				int articleNO = Integer.parseInt(articleMap.get("articleNO"));
				articleVO.setArticleNO(articleNO);
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName");
				articleVO.setParentNO(0);
				articleVO.setId("hong");
				articleVO.setTitle(title);
				articleVO.setContent(content);
				articleVO.setImageFileName(imageFileName);
				boardService.modArticle(articleVO); 
				if(imageFileName !=null && imageFileName.length() !=0) {
					String originalFileName = articleMap.get("originalFileName");
					File srcFile = new File(ARTICLE_IMAGE_REPO+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(ARTICLE_IMAGE_REPO+"\\"+articleNO);
					destDir.mkdir();
					FileUtils.moveFileToDirectory(srcFile, destDir, true);//수정된 이미지 파일을 폴더로 이동
					File oldFile = new File(ARTICLE_IMAGE_REPO+"\\"+articleNO+"\\"+originalFileName);
					oldFile.delete();//기존 파일 삭제
				}
				PrintWriter pw = response.getWriter();
				pw.print("<script> alert('글을 수정하였습니다.'); location.href='" + request.getContextPath()
										+ "/board/viewArticle.do?articleNO=" + articleNO+"'; </script>");
				return;
				
			}else if(action.equals("/removeArticle.do")) {
				int articleNO = Integer.parseInt(request.getParameter("articleNO"));
				List<Integer> articleNOList = boardService.removeArticle(articleNO);
				for(int _articleNO : articleNOList) {
					File imgDir = new File(ARTICLE_IMAGE_REPO+"\\"+_articleNO);
					if(imgDir.exists()) FileUtils.deleteDirectory(imgDir);
				}
				PrintWriter pw = response.getWriter();
				pw.print("<script> alert('글을 삭제했습니다.'); location.href='"+request.getContextPath()+"/board/listArticles.do'; </script>");
				return;
				
			}else if(action.equals("/replyForm.do")) {//답글창 요청 시 미리 부모글 번호를 세션에 저장
				int parentNO = Integer.parseInt(request.getParameter("parentNO"));
				session = request.getSession();
				session.setAttribute("parentNO", parentNO);
				nextPage = "/board01/replyForm.jsp";
				
			}else if(action.equals("/addReply.do")) {
				session = request.getSession();
				int parentNO = (Integer) session.getAttribute("parentNO");
				session.removeAttribute("parentNO");
				Map<String, String> articleMap = upload(request, response);
				String title = articleMap.get("title");
				String content = articleMap.get("content");
				String imageFileName = articleMap.get("imageFileName");
				articleVO.setParentNO(parentNO);//답글의 부모 글 번호 설정
				articleVO.setId("lee");
				articleVO.setTitle(title);
				articleVO.setContent(content);
				articleVO.setImageFileName(imageFileName);
				int articleNO = boardService.addReply(articleVO);
				if(imageFileName != null && imageFileName.length() != 0 ) {
					File srcFile = new File(ARTICLE_IMAGE_REPO+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(ARTICLE_IMAGE_REPO+"\\"+articleNO);
					destDir.mkdirs();//파일 디렉토리 생성
					FileUtils.moveFileToDirectory(srcFile, destDir, true);
				}
				PrintWriter pw = response.getWriter();
				pw.print("<script> alert('답글을 추가했습니다.'); location.href='"
							+request.getContextPath()+"/board/viewArticle.do?articleNO="+articleNO+"'; </script>");
				return;
			}
			RequestDispatcher dispatch = request.getRequestDispatcher(nextPage);
			dispatch.forward(request, response);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private Map<String, String> upload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> articleMap = new HashMap<String, String>();
		String encoding = "utf-8";
		File currentDirPath = new File(ARTICLE_IMAGE_REPO);
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setRepository(currentDirPath);
		factory.setSizeThreshold(1024 * 1024);
		ServletFileUpload upload = new ServletFileUpload(factory);
		try {
			List items = upload.parseRequest(request);
			for(int i=0; i < items.size(); i++) {
				FileItem fileItem = (FileItem) items.get(i);
				if(fileItem.isFormField()) {
					System.out.println(fileItem.getFieldName()+ "=" + fileItem.getString(encoding));
					articleMap.put(fileItem.getFieldName(), fileItem.getString(encoding));
				}else {
					System.out.println("파라미터 이름:" + fileItem.getFieldName());
					System.out.println("파일이름:"+ fileItem.getName());
					System.out.println("파일크기:" + fileItem.getSize() + "bytes");
					if(fileItem.getSize() > 0) {
						int idx = fileItem.getName().lastIndexOf("\\");
						if(idx == -1) {
							idx = fileItem.getName().lastIndexOf("/");
						}
						String fileName = fileItem.getName().substring(idx + 1);
						articleMap.put(fileItem.getFieldName(), fileName);
						File uploadFile = new File(currentDirPath + "\\temp\\" + fileName);
						fileItem.write(uploadFile);
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return articleMap;
	}
}
