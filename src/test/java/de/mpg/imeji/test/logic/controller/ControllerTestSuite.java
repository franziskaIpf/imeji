package de.mpg.imeji.test.logic.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.businesscontroller.InvitationBusinessControllerTest;
import util.SuperTestSuite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
    InvitationBusinessControllerTest.class/*
                                           * , ItemControllerTestClass.class,
                                           * ShareControllerTestClass.class,
                                           * SpaceControllerTestClass.class,
                                           * StatisticsControllerTestClass.class,
                                           * UserControllerTestClass.class,
                                           * RegistrationBusinessControllerTest.class
                                           */})
public class ControllerTestSuite extends SuperTestSuite {

}
