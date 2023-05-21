package team6.car.member.service;

import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import team6.car.apartment.domain.Apartment;
import team6.car.member.DTO.MemberProfileDto;
import team6.car.member.DTO.ReportDto;
import team6.car.member.DTO.getReportDto;
import team6.car.member.domain.Complaint;
import team6.car.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team6.car.member.DTO.UserDto;
import team6.car.device.domain.Device;
import team6.car.member.repository.ComplaintRepository;
import team6.car.member.response.Message;
import team6.car.member.response.StatusEnum;
import team6.car.vehicle.domain.Vehicle;
import team6.car.apartment.repository.ApartmentRepository;
import team6.car.device.repository.DeviceRepository;
import team6.car.vehicle.repository.VehicleRepository;
import team6.car.member.repository.MemberRepository;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service //서비스 스프링 빈으로 등록
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final ApartmentRepository apartmentRepository;
    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;
    private final ComplaintRepository complaintRepository;

    /**회원 가입**/
    @Override
    public Member register(UserDto userDto) {
        //아파트 정보 저장
        Apartment apartment = new Apartment();
        apartment.setApartment_name(userDto.getApartment_name());
        apartmentRepository.save(apartment);

        //사용자 정보 저장
        Member member = new Member();
        member.setApartment(apartment); //FK 설정
        member.setName(userDto.getName());
        member.setPhone_number(userDto.getPhone_number());
        member.setEmail(userDto.getEmail());
        member.setPassword(userDto.getPassword());
        member.setAddress(userDto.getAddress());
        memberRepository.save(member);

        //디바이스 정보 저장
        Device device = new Device();
        device.setDevice_id(userDto.getDevice_id());
        deviceRepository.save(device);

        //차량 정보 저장
        Vehicle vehicle = new Vehicle();
        vehicle.setMember(member); //FK
        vehicle.setDevice(device); //FK
        vehicle.setVehicle_number(userDto.getVehicle_number());
        vehicle.setVehicle_model(userDto.getVehicle_model());
        vehicle.setVehicle_color(userDto.getVehicle_color());
        vehicleRepository.save(vehicle);

        return member;
    }

    /**신고하기**/
    @Override
    public Complaint report(ReportDto reportDto) throws Exception{
        //차량 번호로 차량 정보 가져온 후 외래키로 있는 member의 id값 저장
        Vehicle vehicle = vehicleRepository.findByVehicleNumber(reportDto.getVehicle_number()).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "차량 정보가 존재하지 않습니다.");
        });
        Long memberId = vehicle.getMember().getMember_id();
        //member id값 이용해 회원 정보 가져옴
        Member member = memberRepository.findById(memberId).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보가 존재하지 않습니다.");
        });
        //회원 신고 당한 횟수 + 1 해서 저장
        member = memberRepository.updateComplaintNumber(member);
        member = memberRepository.update(member);

        //complaint_info에 저장
        Complaint complaint = new Complaint();
        complaint.setComplaint_contents(reportDto.getComplaint_contents());
        complaint.setMember(member);
        complaintRepository.save(complaint);
        return complaint;
    }


    /**member_id를 가지고 있는 Complaint_contents들을 가져옴**/
    public List<String> getComplaintContentsByMemberId(Long memberId) {
        List<Complaint> complaints = complaintRepository.findByMemberId(memberId);

        List<String> complaintContentsList = new ArrayList<>();
        for (Complaint complaint : complaints) {
            complaintContentsList.add(complaint.getComplaint_contents());
        }
        return complaintContentsList;
    }

    /**신고 내용 조회**/
    @Override
    public getReportDto getReportInfo(Long id) throws Exception {
        Member member = memberRepository.findById(id).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보가 존재하지 않습니다.");
        });
        Complaint complaint = complaintRepository.findComplaintByMemberId(id).orElseGet(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 당한 내역이 존재하지 않습니다.");
        });

        List<String> complaintContentsList = getComplaintContentsByMemberId(id);
        getReportDto getReportDto = new getReportDto(complaintContentsList, member.getNumber_of_complaints());
        return getReportDto;
    }
}
