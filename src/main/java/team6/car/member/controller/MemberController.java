package team6.car.member.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;
import team6.car.apartment.repository.ApartmentRepository;
import team6.car.device.repository.DeviceRepository;
import team6.car.member.DTO.*;
import team6.car.member.domain.Complaint;
import team6.car.member.domain.Member;
import team6.car.member.repository.MemberRepository;
import team6.car.member.response.Message;
import team6.car.member.response.StatusEnum;
import team6.car.member.service.MemberServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team6.car.vehicle.domain.Vehicle;
import team6.car.vehicle.repository.VehicleRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@RequiredArgsConstructor //의존성 주입
@RestController // annotation 역할 :  spring bean으로 해당 클래스를 spring-container에서 관리
public class MemberController {
    private final MemberServiceImpl memberService;
    private final MemberRepository memberRepository;
    private final ApartmentRepository apartmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;

    @ApiOperation(value="회원가입")
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(회원가입 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @PostMapping("/members")//회원가입
    public ResponseEntity<Message> register(@RequestBody UserDto userDto) {
        String message;
        StatusEnum status;

        if (userDto.getVehicle_model()==null || userDto.getName()==null || userDto.getApartment_name()==null || userDto.getEmail()==null || userDto.getAddress()==null || userDto.getPhone_number()==null ||userDto.getVehicle_number()==null || userDto.getVehicle_color()==null || userDto.getPassword()==null || userDto.getPw_check()==null || userDto.getDevice_id()==null){
            status = StatusEnum.BAD_REQUEST;
            message = "입력하지 않은 정보가 있는지 확인하세요.";
        }
        /**이미 존재하는 이메일로 회원가입 요청 시 -> 예외 발생**/
        else if(memberRepository.findByEmail(userDto.getEmail()).isPresent()){
            status = StatusEnum.BAD_REQUEST;
            message = "회원 정보가 존재합니다.";
        }
        //비밀번호 같은지 확인
        else if(!Objects.equals(userDto.getPassword(), userDto.getPw_check())){
            status = StatusEnum.BAD_REQUEST;
            message = "비밀번호가 일치하지 않습니다.";
        }
        /**이미 존재하는 차량으로 회원가입 요청 시 -> 예외 발생**/
        else if(vehicleRepository.findByVehicleNumber(userDto.getVehicle_number()).isPresent()){
            status = StatusEnum.BAD_REQUEST;
            message = "이미 등록된 차량입니다.";
        }
        /**이미 존재하는 디바이스으로 회원가입 요청 시 -> 예외 발생**/
        else if(deviceRepository.findById(userDto.getDevice_id()).isPresent()){
            status = StatusEnum.BAD_REQUEST;
            message = "이미 등록된 디바이스입니다.";
        }
        else{
            Member member = memberService.register(userDto);
            status = StatusEnum.OK;
            message = "회원가입에 성공하셨습니다.";
        }
        Message responseMessage = new Message();
        responseMessage.setStatus(status);
        responseMessage.setMessage(message);
        responseMessage.setData(null);
        ResponseEntity<Message> response = ResponseEntity.status(status.getStatusCode()).body(responseMessage);
        return response;
    }

    @ApiOperation(value="로그인")
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(로그인 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST(이메일과 비밀번호를 확인해주세요)"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @PostMapping("/members/login") //로그인
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto, HttpSession session) throws Exception{
        String email = loginDto.getEmail();
        String password = loginDto.getPassword();
        Member member = memberRepository.findByEmail(email).orElseThrow();
        Vehicle vehicle = vehicleRepository.findByMemberId(member.getMember_id()).orElseThrow(()->new RuntimeException("차량 정보를 찾을 수 없습니다."));
        Message message = new Message();
        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        if (member.getPassword().equals(password)){
            session.setAttribute("member", member);
            message.setStatus(StatusEnum.OK);
            message.setMessage("로그인 성공");
            ReturnId returnId = new ReturnId(member.getMember_id(), member.getApartment().getApartment_name(), vehicle.getDevice().getDevice_id(), vehicle.getVehicle_id());
            message.setData(returnId);
            return new ResponseEntity<>(message, headers, HttpStatus.OK);
        } else {
            message.setStatus(StatusEnum.NOT_FOUND);
            message.setMessage("이메일과 비밀번호를 확인해주세요");
            return new ResponseEntity<>(message, headers, HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value="로그아웃")
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(로그아웃 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @GetMapping("/members/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }
        Message message = new Message();
        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        message.setStatus(StatusEnum.OK);
        message.setMessage("로그아웃 성공");
        return new ResponseEntity<>(message, headers, HttpStatus.OK);
    }

    @ApiOperation(value="회원 정보 조회")
    @ApiImplicitParam(
            name = "id",
            value = "회원 id",
            required = true,
            dataType = "long",
            paramType = "path"
    )
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(회원 정보 조회 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @GetMapping("/members/{id}") //회원 정보 조회
    public ResponseEntity<Message> getMemberById(@PathVariable Long id) throws Exception {
        StatusEnum status;
        String message;

        Member member = memberRepository.findById(id).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보가 존재하지 않습니다.");
        });
        Vehicle vehicle = vehicleRepository.findByMemberId(id).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "차량 정보가 존재하지 않습니다.");
        });

        MemberProfileDto memberProfileDto = new MemberProfileDto(member.getEmail(), vehicle.getVehicle_number(), member.getAddress(), member.getNumber_of_complaints());

        try {
            message = "회원 정보를 조회합니다.";
            status = StatusEnum.OK;
            Message responseMessage = new Message();
            responseMessage.setStatus(status);
            responseMessage.setMessage(message);
            responseMessage.setData(memberProfileDto);
            ResponseEntity<Message> response = ResponseEntity.status(status.getStatusCode()).body(responseMessage);
            return response;
        } catch (IllegalArgumentException e) {
            // 잘못된 요청인 경우 (BAD_REQUEST)
            message = "잘못된 요청입니다.";
            status = StatusEnum.BAD_REQUEST;
            Message responseMessage = new Message();
            responseMessage.setStatus(status);
            responseMessage.setMessage(message);
            responseMessage.setData(null);
            ResponseEntity<Message> response = ResponseEntity.status(status.getStatusCode()).body(responseMessage);
            return response;
        } catch (Exception e) {
            // 내부 서버 오류인 경우 (INTERNAL_SERVER_ERROR)
            message = "회원 정보 조회에 실패하였습니다.";
            status = StatusEnum.INTERNAL_SERVER_ERROR;
            Message responseMessage = new Message();
            responseMessage.setStatus(status);
            responseMessage.setMessage(message);
            responseMessage.setData(null);
            ResponseEntity<Message> response = ResponseEntity.status(status.getStatusCode()).body(responseMessage);
            return response;
        }
    }

    @ApiOperation(value="신고하기",notes="비매너 주민 신고 기능")
    @ApiImplicitParam(
            name = "id",
            value = "회원 id",
            required = true,
            dataType = "long"
    )
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(신고하기 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @PostMapping("/members/complaints") //신고하기
    public ResponseEntity<?> report(@RequestBody ReportDto reportDto) throws Exception {
        Complaint complaint = memberService.report(reportDto);
        Message message = new Message();
        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        message.setStatus(StatusEnum.OK);
        message.setMessage("신고하기 성공");
        message.setData(complaint);
        return new ResponseEntity<>(message, headers, HttpStatus.OK);
    }

    @ApiOperation(value="신고 내용 조회",notes="본인의 신고 당한 내역 조회")
    @ApiImplicitParam(
            name = "id",
            value = "회원 id",
            required = true,
            dataType = "long",
            paramType = "path"
    )
    @ApiResponses({
            @ApiResponse(code = 200,message = "OK(신고 내용 조회 성공)"),
            @ApiResponse(code = 400, message="BAD_REQUEST"),
            @ApiResponse(code = 404, message="NOT_FOUND"),
            @ApiResponse(code = 500, message="INTERNAL_SERVER_ERROR")
    })
    @GetMapping("/members/{id}/complaints") //신고 내용 조회
    public ResponseEntity<?> getReportInfo(@PathVariable Long id) throws Exception {
        getReportDto report = memberService.getReportInfo(id);
        Message message = new Message();
        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        message.setStatus(StatusEnum.OK);
        message.setMessage("신고 내용 조회 성공");
        message.setData(report);
        return new ResponseEntity<>(message, headers, HttpStatus.OK);
    }
}
