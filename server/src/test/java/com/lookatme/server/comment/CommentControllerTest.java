package com.lookatme.server.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lookatme.server.auth.dto.MemberPrincipal;
import com.lookatme.server.auth.jwt.JwtTokenizer;
import com.lookatme.server.auth.utils.MemberAuthorityUtils;
import com.lookatme.server.board.dto.SimpleBoardResponseDto;
import com.lookatme.server.board.entity.Board;
import com.lookatme.server.comment.controller.CommentController;
import com.lookatme.server.comment.dto.CommentPatchDto;
import com.lookatme.server.comment.dto.CommentPostDto;
import com.lookatme.server.comment.dto.CommentResponseDto;
import com.lookatme.server.comment.entity.Comment;
import com.lookatme.server.comment.service.CommentService;
import com.lookatme.server.common.dto.MultiResponseDto;
import com.lookatme.server.config.CustomTestConfiguration;
import com.lookatme.server.config.WithAuthMember;
import com.lookatme.server.member.entity.Account;
import com.lookatme.server.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        CustomTestConfiguration.class,
        MemberAuthorityUtils.class,
        JwtTokenizer.class
})
@WebMvcTest(controllers = CommentController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfigurer.class)})
@MockBean(JpaMetamodelMappingContext.class)
@AutoConfigureRestDocs
public class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CommentService commentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithAuthMember
    public void postCommentTest() throws Exception {
        //given
        CommentPostDto commentPostDto = CommentPostDto.builder()
                .content("?????? ??????")
                .boardId("1")
                .build();

        Member member = Member.builder()
                .memberId(1L)
                .account(new Account("email@com"))
                .nickname("?????? ????????? ?????????")
                .profileImageUrl("?????? ????????? ????????? ?????? URL")
                .height(180)
                .weight(70)
                .build();

        String content = objectMapper.writeValueAsString(commentPostDto);

        Comment comment = Comment.builder()
                .commentId(1L)
                .content(commentPostDto.getContent())
                .member(member)
                .build();

        Board board = Board.builder()
                .boardId(1)
                .userImage("image")
                .content("????????? ??????")
                .likeCnt(0)
                .build();

        comment.addMember(member);
        comment.addBoard(board);

        CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createdDate(comment.getCreatedDate())
                .updatedDate(comment.getUpdatedDate())
                .nickname(comment.getMember().getNickname())
                .profileImageUrl(comment.getMember().getProfileImageUrl())
                .board(SimpleBoardResponseDto.of(comment.getBoard()))
                .build();

        given(commentService.createComment(Mockito.any(CommentPostDto.class), Mockito.any())).willReturn(commentResponseDto);

        //when
        ResultActions actions =
                mockMvc.perform(post("/comment")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .content(content)
                );
        //then
        actions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(comment.getCommentId()))
                .andExpect(jsonPath("$.content").value(comment.getContent()))
                .andExpect(jsonPath("$.nickname").value(comment.getMember().getNickname()))
                .andExpect(jsonPath("$.profileImageUrl").value(comment.getMember().getProfileImageUrl()))
                .andDo(document("create-comment",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        requestFields(
                                List.of(
                                        fieldWithPath("boardId").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????")
                                )
                        ),
                        responseFields(
                                List.of(
                                        fieldWithPath("commentId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
                                        fieldWithPath("createdDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("updatedDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ????????? ?????????"),
                                        fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? ??????"),
                                        fieldWithPath("board").type(JsonFieldType.OBJECT).description("????????? ?????????"),
                                        fieldWithPath("board.boardId").type(JsonFieldType.NUMBER).description("????????? ??????"),
                                        fieldWithPath("board.userImage").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.content").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.createdAt").type(JsonFieldType.NULL).description("????????? ??????"),
                                        fieldWithPath("board.modifiedAt").type(JsonFieldType.NULL).description("????????? ?????????"),
                                        fieldWithPath("board.likeCnt").type(JsonFieldType.NUMBER).description("????????? ?????????")

                                )
                        )
                ));
    }

    @Test
    @WithAuthMember
    public void patchCommentTest() throws Exception {
        //given
        CommentPatchDto commentPatchDto = CommentPatchDto.builder()
                .content("?????? ??????")
                .build();

        String content = objectMapper.writeValueAsString(commentPatchDto);

        Comment comment = Comment.builder()
                .commentId(1L)
                .content(commentPatchDto.getContent())
                .build();

        Member member = Member.builder()
                .memberId(1L)
                .account(new Account("email@com"))
                .nickname("?????? ????????? ?????????")
                .profileImageUrl("?????? ????????? ????????? ?????? URL")
                .height(180)
                .weight(70)
                .build();

        Board board = Board.builder()
                .boardId(1)
                .userImage("image")
                .content("????????? ??????")
                .likeCnt(0)
                .build();

        comment.addMember(member);
        comment.addBoard(board);

        CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createdDate(comment.getCreatedDate())
                .updatedDate(comment.getUpdatedDate())
                .nickname(comment.getMember().getNickname())
                .profileImageUrl(comment.getMember().getProfileImageUrl())
                .board(SimpleBoardResponseDto.of(comment.getBoard()))
                .build();

        given(commentService.editComment(Mockito.anyLong(), Mockito.any(CommentPatchDto.class), Mockito.any())).willReturn(commentResponseDto);

        //when
        Long commentId = comment.getCommentId();
        ResultActions actions =
                mockMvc.perform(patch("/comment/{commentId}", commentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .content(content)
                );
        //then
        actions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(comment.getCommentId()))
                .andExpect(jsonPath("$.content").value(comment.getContent()))
                .andExpect(jsonPath("$.nickname").value(comment.getMember().getNickname()))
                .andExpect(jsonPath("$.profileImageUrl").value(comment.getMember().getProfileImageUrl()))
                .andDo(document("patch-comment",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("commentId").description("?????? ?????????")
                        ),
                        requestFields(
                                List.of(
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ?????? ??????")
                                )
                        ),
                        responseFields(
                                List.of(
                                        fieldWithPath("commentId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
                                        fieldWithPath("createdDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("updatedDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ????????? ?????????"),
                                        fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? ??????"),
                                        fieldWithPath("board").type(JsonFieldType.OBJECT).description("????????? ?????????"),
                                        fieldWithPath("board.boardId").type(JsonFieldType.NUMBER).description("????????? ??????"),
                                        fieldWithPath("board.userImage").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.content").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.createdAt").type(JsonFieldType.NULL).description("????????? ??????"),
                                        fieldWithPath("board.modifiedAt").type(JsonFieldType.NULL).description("????????? ?????????"),
                                        fieldWithPath("board.likeCnt").type(JsonFieldType.NUMBER).description("????????? ?????????")
                                )
                        )
                ));
    }

    @Test
    public void getCommentTest() throws Exception {
        //given
        Long commentId = 1L;
        Comment comment = Comment.builder()
                .commentId(commentId)
                .content("?????? ??????")
                .build();

        Member member = Member.builder()
                .memberId(1L)
                .account(new Account("email@com"))
                .nickname("?????????")
                .profileImageUrl("http://????????????")
                .height(180)
                .weight(70)
                .build();

        Board board = Board.builder()
                .boardId(1)
                .userImage("image")
                .content("????????? ??????")
                .likeCnt(0)
                .build();

        comment.addMember(member);
        comment.addBoard(board);

        CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createdDate(comment.getCreatedDate())
                .updatedDate(comment.getUpdatedDate())
                .nickname(comment.getMember().getNickname())
                .profileImageUrl(comment.getMember().getProfileImageUrl())
                .board(SimpleBoardResponseDto.of(comment.getBoard()))
                .build();

        given(commentService.findComment(Mockito.anyLong())).willReturn(commentResponseDto);

        //when
        ResultActions actions =
                mockMvc.perform(get("/comment/{commentId}", comment.getCommentId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                );
        //then
        actions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(comment.getCommentId()))
                .andExpect(jsonPath("$.content").value(comment.getContent()))
                .andExpect(jsonPath("$.nickname").value(comment.getMember().getNickname()))
                .andExpect(jsonPath("$.profileImageUrl").value(comment.getMember().getProfileImageUrl()))
                .andDo(document("get-comment",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("commentId").description("?????? ?????????")
                        ),
                        responseFields(
                                List.of(
                                        fieldWithPath("commentId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
                                        fieldWithPath("createdDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("updatedDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("?????? ????????? ?????????"),
                                        fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? ??????"),
                                        fieldWithPath("board").type(JsonFieldType.OBJECT).description("????????? ?????????"),
                                        fieldWithPath("board.boardId").type(JsonFieldType.NUMBER).description("????????? ??????"),
                                        fieldWithPath("board.userImage").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.content").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("board.createdAt").type(JsonFieldType.NULL).description("????????? ??????"),
                                        fieldWithPath("board.modifiedAt").type(JsonFieldType.NULL).description("????????? ?????????"),
                                        fieldWithPath("board.likeCnt").type(JsonFieldType.NUMBER).description("????????? ?????????")
                                )
                        )
                ));
    }

    @Test
    public void getCommentsTest() throws Exception {
        //given
        Comment comment1 = Comment.builder()
                .commentId(1L)
                .content("?????? ??????")
                .build();

        Comment comment2 = Comment.builder()
                .commentId(2L)
                .content("?????? ??????")
                .build();

        Member member = Member.builder()
                .memberId(1L)
                .account(new Account("email@com"))
                .nickname("?????????")
                .profileImageUrl("http://????????????")
                .height(180)
                .weight(70)
                .build();

        long boardId = 1;
        Board board = Board.builder()
                .boardId(boardId)
                .userImage("image")
                .content("????????? ??????")
                .likeCnt(0)
                .build();

        comment1.addMember(member);
        comment1.addBoard(board);
        comment2.addMember(member);
        comment2.addBoard(board);

        List<Comment> commentList = List.of(comment1, comment2);

        PageImpl<Comment> comments = new PageImpl<>(commentList,
                PageRequest.of(0, 10, Sort.by("createdDate")), 2);

        CommentResponseDto commentResponseDto1 = CommentResponseDto.builder()
                .commentId(comment1.getCommentId())
                .content(comment1.getContent())
                .createdDate(comment1.getCreatedDate())
                .updatedDate(comment1.getUpdatedDate())
                .nickname(comment1.getMember().getNickname())
                .profileImageUrl(comment1.getMember().getProfileImageUrl())
                .board(SimpleBoardResponseDto.of(comment1.getBoard()))
                .build();

        CommentResponseDto commentResponseDto2 = CommentResponseDto.builder()
                .commentId(comment2.getCommentId())
                .content(comment2.getContent())
                .createdDate(comment2.getCreatedDate())
                .updatedDate(comment2.getUpdatedDate())
                .nickname(comment2.getMember().getNickname())
                .profileImageUrl(comment2.getMember().getProfileImageUrl())
                .board(SimpleBoardResponseDto.of(comment2.getBoard()))
                .build();

        List<CommentResponseDto> commentResponseDtos = List.of(commentResponseDto1, commentResponseDto2);

        given(commentService.getComments(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt())).willReturn(new MultiResponseDto(commentResponseDtos, comments));

        //when
        ResultActions actions =
                mockMvc.perform(get("/comment/board/{boardId}", boardId)
                        .param("page", "1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                );
        //then
        actions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].commentId").value(comment1.getCommentId()))
                .andExpect(jsonPath("$.data[0].content").value(comment1.getContent()))
                .andExpect(jsonPath("$.data[0].nickname").value(comment1.getMember().getNickname()))
                .andExpect(jsonPath("$.data[0].profileImageUrl").value(comment1.getMember().getProfileImageUrl()))
                .andExpect(jsonPath("$.data[1].commentId").value(comment2.getCommentId()))
                .andExpect(jsonPath("$.data[1].content").value(comment2.getContent()))
                .andExpect(jsonPath("$.data[1].nickname").value(comment2.getMember().getNickname()))
                .andExpect(jsonPath("$.data[1].profileImageUrl").value(comment2.getMember().getProfileImageUrl()))
                .andDo(document("get-comments",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("boardId").description("????????? ?????????")
                        ),
                        requestParameters(
                                parameterWithName("page").description("????????? ??????"),
                                parameterWithName("size").description("????????? ??????")
                        ),
                        responseFields(
                                List.of(
                                        fieldWithPath("data").type(JsonFieldType.ARRAY).description("?????? ??????"),
                                        fieldWithPath("data[].commentId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("data[].content").type(JsonFieldType.STRING).description("?????? ??????"),
                                        fieldWithPath("data[].createdDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("data[].updatedDate").type(JsonFieldType.NULL).description("?????? ?????????"),
                                        fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("?????? ????????? ?????????"),
                                        fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).description("?????? ????????? ????????? ??????"),
                                        fieldWithPath("data[].board").type(JsonFieldType.OBJECT).description("????????? ?????????"),
                                        fieldWithPath("data[].board.boardId").type(JsonFieldType.NUMBER).description("????????? ??????"),
                                        fieldWithPath("data[].board.userImage").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("data[].board.content").type(JsonFieldType.STRING).description("????????? ?????????"),
                                        fieldWithPath("data[].board.createdAt").type(JsonFieldType.NULL).description("????????? ??????"),
                                        fieldWithPath("data[].board.modifiedAt").type(JsonFieldType.NULL).description("????????? ?????????"),
                                        fieldWithPath("data[].board.likeCnt").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                                        fieldWithPath("pageInfoDto").type(JsonFieldType.OBJECT).description("????????? ??????"),
                                        fieldWithPath("pageInfoDto.page").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("pageInfoDto.size").type(JsonFieldType.NUMBER).description("?????? ????????? ?????????"),
                                        fieldWithPath("pageInfoDto.totalElements").type(JsonFieldType.NUMBER).description("?????? ?????????"),
                                        fieldWithPath("pageInfoDto.totalPages").type(JsonFieldType.NUMBER).description("?????? ?????????")
                                )
                        )
                ));
    }

    @Test
    @WithAuthMember
    public void deleteCommentTest() throws Exception {
        //given
        Long commentId = 1L;
        doNothing().when(commentService).deleteComment(Mockito.anyLong(), Mockito.any(MemberPrincipal.class));

        //when
        ResultActions actions =
                mockMvc.perform(delete("/comment/{commentId}", commentId));
        //then
        actions
                .andExpect(status().isNoContent())
                .andDo(document("delete-comment",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("commentId").description("?????? ?????????")
                        )
                ));
    }
}
