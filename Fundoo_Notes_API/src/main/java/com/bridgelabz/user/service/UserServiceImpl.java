package com.bridgelabz.user.service;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bridgelabz.exception.UserException;
import com.bridgelabz.response.Response;
import com.bridgelabz.user.dto.LoginDto;
import com.bridgelabz.user.dto.UserDto;
import com.bridgelabz.user.model.Email;
import com.bridgelabz.user.model.User;
import com.bridgelabz.user.repository.UserRepository;
import com.bridgelabz.utility.EncryptUtil;
import com.bridgelabz.utility.ITockenGenerator;
import com.bridgelabz.utility.MailUtil;

@Service
public class UserServiceImpl implements UserService {
	@Autowired(required = true)
	ITockenGenerator tockenGenerator;

	@Autowired(required = true)
	private UserRepository userRepository;

	@Autowired
	private MailUtil mailsender;

	@Autowired(required = true)
	private ModelMapper modelMapper;

	@Autowired(required = true)
	PasswordEncoder encoder;

	@Autowired
	EncryptUtil encryptUtil;

	@Override
	public Response registration(UserDto userDto, StringBuffer requestUrl) {
//		Optional<User> optinalUser = userRepository.findByEmailId(userDto.getEmailId());
		boolean isemail = userRepository.findByEmailId(userDto.getEmailId()).isPresent();
		if (!isemail) {
			User user = modelMapper.map(userDto, User.class);
			user.setPassword(encoder.encode(userDto.getPassword()));
			User saveUser = userRepository.save(user);
			try {
				String tocken = tockenGenerator.generateTocken(saveUser.getUserId());
				String activationUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/")) + "/resetPassword/" + tocken;
				Email email = new Email();
				email.setTo("kumar.arjun6515@gmail.com");
				email.setSubject("Account Verification");
				email.setBody("Please verify EmailId using belowl link \n" + activationUrl);
				mailsender.send(email);
				return new Response(200, "Email send successfuly", null);
			} catch (Exception e) {
				e.printStackTrace();
				throw new UserException("fill carefully data");
			}
		} else {
			throw new UserException("EmailId already exist");
		}
	}

	@Override
	public String loginUser(LoginDto loginDto) {
		Optional<User> isEmail = userRepository.findByEmailId(loginDto.getEmailId());
		 if (isEmail.isPresent()) {
			User userSaved = isEmail.get();
			 if (userSaved.isVerify() == true) {
				boolean present = encoder.matches(loginDto.getPassword(), userSaved.getPassword());
				 if (present == true) {
					try {
						String token = tockenGenerator.generateTocken(isEmail.get().getUserId());
						System.out.println(token);
					} catch (IllegalArgumentException | UnsupportedEncodingException e) {
						throw new UserException();
					}
					return "login successfull";
				} else {
					throw new UserException(" Wrong Password");
				}
			}
			throw new UserException("somthing not matches");
		}
		return null;
	}

	@Override
	public Response forgotPassword(String emailId, StringBuffer requestUrl) {
		Optional<User> userOptional = userRepository.findByEmailId(emailId);
		if (userOptional.isPresent()) {
			User userSaved = userOptional.get();
		 try {
				String tocken = tockenGenerator.generateTocken(userSaved.getUserId());
				String activationUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/")) + "/resetPassword/" + tocken;
				Email email = new Email();
				email.setTo("kumar.arjun6515@gmail.com");
				email.setSubject("Account Verification");
				email.setBody("Change Password . \n" + activationUrl);
				mailsender.send(email);
				return new Response(200, "Email send successfuly", null);
			   } 
		catch (Exception e) {
		        e.printStackTrace();
				 throw new UserException("internal server error");
			      }
		      }
		
		else {
			  throw new UserException("User not present..");
		     }
	    }

	@Override
	public Response reSetPassword(String tocken, String password) {

		String id = tockenGenerator.verifyTocken(tocken);
		Optional<User> userOptional = userRepository.findById(id);
		  if (userOptional.isPresent()) {
			  User user = userOptional.get();
			  user.setPassword(encoder.encode(password));
		      userRepository.save(user);
			  return new Response(200, "Resetpassword successfully", null);
		     }
		else {
			  throw new UserException("please enter vailid data");
		      }
	     }

	@Override
	public String userValidation(String tocken) {

		String id = tockenGenerator.verifyTocken(tocken);
		Optional<User> optuser = userRepository.findById(id);
		if (optuser.isPresent()) {
			User user = optuser.get();
			user.setVerify(true);
			userRepository.save(user);
			return "user verified";
		   } 
		else {
			return "user not verified";
		    }
	    }

}
