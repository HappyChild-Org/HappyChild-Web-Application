package com.c0324.casestudym5.service.impl;

import com.c0324.casestudym5.dto.TeamDTO;
import com.c0324.casestudym5.model.*;
import com.c0324.casestudym5.repository.TeacherRepository;
import com.c0324.casestudym5.repository.TeamRepository;
import com.c0324.casestudym5.service.*;
import com.c0324.casestudym5.util.CommonMapper;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final StudentService studentService;
    private final TeacherRepository teacherRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    public TeamServiceImpl(TeamRepository teamRepository, StudentService studentService,
                           TeacherRepository teacherRepository, NotificationService notificationService, MailService mailService) {
        this.teamRepository = teamRepository;
        this.studentService = studentService;
        this.teacherRepository = teacherRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Override
    public List<Team> findAll() {
        return teamRepository.findAll();
    }

    @Override
    public Team save(Team team) {
        return teamRepository.save(team);
    }

    @Override
    public Team findById(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    @Override
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }

    @Override
    public Page<TeamDTO> getPageTeams(int page, String keyword, User user) {
        Pageable pageable = PageRequest.of(page, 5);
        Teacher teacher = teacherRepository.findTeacherByUserEmail(user.getEmail());
        Page<Team> teams;
        if (keyword.isEmpty()) {
            teams = teamRepository.findTeamsByTeacherId(teacher.getId(), pageable);
        } else {
            teams = teamRepository.searchTeamByNameAndTeacherId(teacher.getId(), keyword, pageable);
        }
        List<Team> filteredTeams = teams.stream()
                .filter(team -> !team.getStudents().isEmpty())
                .toList();
        List<TeamDTO> teamDTOs = filteredTeams.stream()
            .map(CommonMapper::mapToTeamDTO)
            .toList();
        return new PageImpl<>(teamDTOs, pageable, filteredTeams.size());
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, User sender) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team != null && sender != null) {
            Topic topic = team.getTopic();
            if (topic != null) {
                topic.setTeam(null);
            }

            List<Student> students = team.getStudents();
            String subject = "Thông báo xóa nhóm - " + team.getName();
            for (Student student : students) {
                student.setTeam(null);
                student.setLeader(false);
                mailService.sendDeleteTeamEmail(student.getUser().getEmail(), subject, sender.getName(), student.getUser().getName(), team.getName());
                Notification notification = Notification.builder()
                        .content(sender.getName() + " đã xóa nhóm " + team.getName())
                        .sender(sender)
                        .receiver(student.getUser())
                        .url(null)
                        .build();
                notificationService.sendNotification(notification);
            }
            studentService.saveAll(students);
            teamRepository.delete(team);
        }
    }

    @Override
    public void createNewTeam(TeamDTO teamDTO, Student currentStudent) {
        Team newTeam = new Team();
        newTeam.setName(teamDTO.getName());
        newTeam.setStudents(List.of(currentStudent));
        teamRepository.save(newTeam);
        currentStudent.setTeam(newTeam);
        currentStudent.setLeader(true);
        studentService.save(currentStudent);
    }

    @Override
    public void registerTeacher(Long teamId, Long teacherId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalStateException("Nhóm không tồn tại"));
        if (team.getTeacher() != null) {
            throw new IllegalStateException("Nhóm đã có giáo viên hướng dẫn!");
        }
        if (team.getTopic() != null && teacherId == 0) {
            throw new IllegalStateException("Đã đăng ký đề tài, không được hủy giáo viên!");
        }
        if (team.getTopic() != null) {
            throw new IllegalStateException("Nhóm đã đăng ký đề tài, không thể đổi giáo viên!");
        }
        if (teacherId == 0) {
            team.setTeacher(null);
            teamRepository.save(team);
        } else {
            Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalStateException("Giáo viên không tồn tại"));
            if (countTeamsByTeacherId(teacherId) >= 5) {
                throw new IllegalStateException("Giáo viên đã đủ số lượng nhóm!");
            }
            team.setTeacher(teacher);
            teamRepository.save(team);
        }
    }

    @Override
    public int countTeamsByTeacherId(Long teacherId) {
        return teamRepository.countByTeacherId(teacherId);
    }
}