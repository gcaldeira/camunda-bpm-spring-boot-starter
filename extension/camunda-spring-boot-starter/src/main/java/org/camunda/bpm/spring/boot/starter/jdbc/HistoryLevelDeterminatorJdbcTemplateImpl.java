package org.camunda.bpm.spring.boot.starter.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.CamundaBpmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class HistoryLevelDeterminatorJdbcTemplateImpl implements HistoryLevelDeterminator, InitializingBean {

  public static HistoryLevelDeterminator createHistoryLevelDeterminator(CamundaBpmProperties camundaBpmProperties, JdbcTemplate jdbcTemplate) {
    final HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setCamundaBpmProperties(camundaBpmProperties);
    determinator.setJdbcTemplate(jdbcTemplate);
    return determinator;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(HistoryLevelDeterminatorJdbcTemplateImpl.class);

  private static final String TABLE_PREFIX_PLACEHOLDER = "{TABLE_PREFIX}";

  protected static final String SQL_TEMPLATE = "SELECT VALUE_ FROM " + TABLE_PREFIX_PLACEHOLDER + "ACT_GE_PROPERTY WHERE NAME_='historyLevel'";

  protected final List<HistoryLevel> historyLevels = new ArrayList<HistoryLevel>(Arrays.asList(new HistoryLevel[] { HistoryLevel.HISTORY_LEVEL_ACTIVITY,
      HistoryLevel.HISTORY_LEVEL_AUDIT, HistoryLevel.HISTORY_LEVEL_FULL, HistoryLevel.HISTORY_LEVEL_NONE }));

  protected String defaultHistoryLevel = new SpringProcessEngineConfiguration().getHistory();

  protected JdbcTemplate jdbcTemplate;

  protected CamundaBpmProperties camundaBpmProperties;

  protected boolean ignoreDataAccessException = true;

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(jdbcTemplate, "a jdbc template must be set");
    Assert.notNull(camundaBpmProperties, "camunda bpm properties must be set");
    String historyLevelDefault = camundaBpmProperties.getHistoryLevelDefault();
    if (StringUtils.hasText(historyLevelDefault)) {
      defaultHistoryLevel = historyLevelDefault;
    }
  }

  @Override
  public String determineHistoryLevel() {
    Integer historyLevelFromDb = null;
    try {
      historyLevelFromDb = jdbcTemplate.queryForObject(getSql(), Integer.class);
      LOGGER.debug("found history '{}' in database", historyLevelFromDb);
    } catch (DataAccessException e) {
      if (ignoreDataAccessException) {
        LOGGER.warn("unable to fetch history level from database: {}", e.getMessage());
        LOGGER.debug("unable to fetch history level from database", e);
      } else {
        throw e;
      }
    }
    return getHistoryLevelFrom(historyLevelFromDb);
  }

  protected String getSql() {
    String tablePrefix = camundaBpmProperties.getDatabase().getTablePrefix();
    if (tablePrefix == null) {
      tablePrefix = "";
    }
    return SQL_TEMPLATE.replace(TABLE_PREFIX_PLACEHOLDER, tablePrefix);
  }

  protected String getHistoryLevelFrom(Integer historyLevelFromDb) {
    String result = defaultHistoryLevel;
    if (historyLevelFromDb != null) {
      for (HistoryLevel historyLevel : historyLevels) {
        if (historyLevel.getId() == historyLevelFromDb.intValue()) {
          result = historyLevel.getName();
          LOGGER.debug("found matching history level '{}'", result);
          break;
        }
      }
    }
    return result;
  }

  /**
   * @return the defaultHistoryLevel
   */
  public String getDefaultHistoryLevel() {
    return defaultHistoryLevel;
  }

  /**
   * @param defaultHistoryLevel
   *          the defaultHistoryLevel to set
   */
  public void setDefaultHistoryLevel(String defaultHistoryLevel) {
    this.defaultHistoryLevel = defaultHistoryLevel;
  }

  /**
   * @return the jdbcTemplate
   */
  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  /**
   * @param jdbcTemplate
   *          the jdbcTemplate to set
   */
  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * @return the ignoreDataAccessException
   */
  public boolean isIgnoreDataAccessException() {
    return ignoreDataAccessException;
  }

  /**
   * @param ignoreDataAccessException
   *          the ignoreDataAccessException to set
   */
  public void setIgnoreDataAccessException(boolean ignoreDataAccessException) {
    this.ignoreDataAccessException = ignoreDataAccessException;
  }

  /**
   * @return the camundaBpmProperties
   */
  public CamundaBpmProperties getCamundaBpmProperties() {
    return camundaBpmProperties;
  }

  /**
   * @param camundaBpmProperties
   *          the camundaBpmProperties to set
   */
  public void setCamundaBpmProperties(CamundaBpmProperties camundaBpmProperties) {
    this.camundaBpmProperties = camundaBpmProperties;
  }

  public void addCustomHistoryLevels(Collection<HistoryLevel> customHistoryLevels) {
    historyLevels.addAll(customHistoryLevels);
  }
}