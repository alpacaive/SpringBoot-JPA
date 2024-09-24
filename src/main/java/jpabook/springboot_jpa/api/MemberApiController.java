package jpabook.springboot_jpa.api;

import jakarta.validation.Valid;
import jpabook.springboot_jpa.domain.Member;
import jpabook.springboot_jpa.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // api를 만들때는 항상 엔티티를 파라미터로 받으면 안됨( ex) v1 )
    // 엔티티를 외부에 노출해서도 안된다

    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    // 1번의 장점 = 클래스 안만들어도 됨 -> 엔티티 고치면 망함
    // 2번의 장점 = 엔티티 바꿔도 api 스펙 안바뀜, 엔티티만 보면 파라미터가 뭔지 알 수 없는데 DTO를 만들어서 했기 때문에 DTO만 보면 알 수 있다.
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

}
