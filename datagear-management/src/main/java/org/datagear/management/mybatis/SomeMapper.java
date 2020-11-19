package org.datagear.management.mybatis;

import org.apache.ibatis.annotations.*;
import org.datagear.management.domain.SaveDataSetEntity;
import org.datagear.management.domain.User;

import java.util.List;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/10/27 11:38
 * @since 1.0.0
 */

public interface SomeMapper {

    @Select("select hcw_rule_id from datagear_html_chart_widget where hcw_id=#{chartId} ")
    Long queryRuleId(@Param("chartId") String chartId);

    @Update("update datagear_html_chart_widget set hcw_rule_id=#{ruleId} where hcw_id=#{chartId} ")
    Integer addRuleToChart(@Param("chartId") String chartId, @Param("ruleId") Long ruleId);

    @Insert("INSERT INTO DATAGEAR_DATA_SET " +
            "( DS_ID, DS_NAME, DS_TYPE, DS_CREATE_USER_ID, DS_CREATE_TIME,DS_AP_ID ) " +
            " VALUES ( #{entity.dsId}, #{entity.dsName}, #{entity.dsType}, #{entity.dsCreateUserId}, " +
            "#{entity.dsCreateTime},#{entity.dsApId} )")
//    @Options( keyProperty="dsId", keyColumn="ds_id")
    Integer insertDataSet(@Param("entity") SaveDataSetEntity entity);

    @Insert("insert into DATAGEAR_DATA_RESULT (DS_ID,DS_RESULT) values (#{dsId} ,#{dsResult} )")
    Integer insertDataResult(@Param("dsId") String dsId, @Param("dsResult") String dsResult);

    @Delete("delete * from DATAGEAR_DATA_RESULT where DS_ID=#{dsId} ")
    Integer deleteDataResult(@Param("dsId") String dsId);

    @Update("update DATAGEAR_DATA_RESULT set DS_RESULT=#{dsResult} where DS_ID=#{dsId}")
    Integer updateDataResult(@Param("dsId") String dsId, @Param("dsResult") String dsResult);

    @Select("select ds_id from DATAGEAR_HCW_DS where hcw_id=#{hcwId} ")
    String queryDsId(@Param("hcwId") String hcwId);

    @Select("select ds_result from DATAGEAR_DATA_RESULT where ds_id =#{dsId} ")
    String queryResult(@Param("dsId") String dsId);

    @Select("select count(0) from datagear_html_chart_widget where hcw_name=#{name} and hcw_create_user_id=#{userId} ")
    int queryChartNameNum(@Param("name") String name, @Param("userId") String userId);

    @Select("select count(0) from datagear_html_chart_widget where hcw_id!=#{id} and hcw_create_user_id=#{userId} and hcw_name=#{name}")
    int queryChartNameById(@Param("id") String id, @Param("userId") String userId, @Param("name") String name);

    @Select("select count(0) from datagear_data_set where ds_name=#{name} and ds_create_user_id=#{userId}")
    int queryDataSetNameNum(@Param("name") String name, @Param("userId") String userId);

    @Select("select count(0) from datagear_data_set where ds_name=#{name} and ds_create_user_id=#{userId} and ds_id!=#{id} ")
    int queryDataSetById(@Param("id") String id, @Param("name") String name, @Param("userId") String userId);

    @Select("select count(0) from datagear_schema where schema_title=#{name} and schema_create_user_id=#{userId}")
    int querySchemaNameNum(@Param("name") String name, @Param("userId") String userId);

    @Select("select count(0) from datagear_schema where schema_title=#{name} and schema_create_user_id=#{userId} and schema_id !=#{id} ")
    int querySchemaNameById(@Param("name") String name, @Param("userId") String userId,@Param("id")String id);

    @Select("select hcw_create_user_id as id from datagear_html_chart_widget where hcw_id=#{chartId}  ")
    User queryUserByChartId(@Param("chartId") String chartId);

    @Select("<script>" +
            "select count(0) from page_element where data_set_id in " +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>"+
            "#{item} "+
            "</foreach>" +
            "</script>")
    Integer deleteChartById(@Param("ids")String[] ids);
}
