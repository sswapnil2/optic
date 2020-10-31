import React from 'react';
import PropTypes from 'prop-types';
import CircularProgress from '@material-ui/core/CircularProgress';
import Typography from '@material-ui/core/Typography';
import Box from '@material-ui/core/Box';
import CheckIcon from '@material-ui/icons/Check';
import { makeStyles } from '@material-ui/core/styles';
import {
  AddedGreen,
  AddedGreenBackground,
  ChangedYellow,
  ChangedYellowBackground,
  ChangedYellowDark,
  OpticBlue,
  OpticBlueLightened,
  OpticBlueReadable,
  secondary,
  UpdatedBlue,
  UpdatedBlueBackground,
} from '../../../theme';
import Grow from '@material-ui/core/Grow';

export function CircularDiffProgress(props) {
  const { total, handled } = props;
  const classes = useStyles();
  const value = (handled / total) * 100;
  const allHandled = total === handled;
  return (
    <Box position="relative" display="inline-flex">
      <Grow in={!allHandled}>
        <CircularProgress
          variant="static"
          value={value}
          style={{ color: UpdatedBlueBackground }}
        />
      </Grow>
      <Box
        top={0}
        left={0}
        bottom={0}
        right={0}
        position="absolute"
        display="flex"
        alignItems="center"
        justifyContent="center"
      >
        {value === 0 && (
          <Typography
            className={classes.label}
            variant="caption"
            component="div"
            color="textSecondary"
          >{`𝚫 ${total}`}</Typography>
        )}
        {value > 0 && total !== handled && (
          <Typography
            className={classes.labelCounting}
            variant="caption"
            component="div"
            color="textSecondary"
          >{`${handled}/${total}`}</Typography>
        )}
        {allHandled && (
          <Grow in={allHandled} appear={true} timeout={1000}>
            <div className={classes.complete}>
              <CheckIcon style={{ color: AddedGreen, width: 15, height: 15 }} />
            </div>
          </Grow>
        )}
      </Box>
    </Box>
  );
}

const useStyles = makeStyles((theme) => ({
  label: {
    fontFamily: 'Ubuntu Mono',
    fontSize: 10,
    backgroundColor: ChangedYellowBackground,
    color: ChangedYellowDark,
    paddingLeft: 3,
    paddingTop: 1,
    paddingRight: 3,
    borderRadius: 4,
  },
  labelCounting: {
    fontFamily: 'Ubuntu Mono',
    fontSize: 8,
    backgroundColor: UpdatedBlueBackground,
    color: UpdatedBlue,
    paddingLeft: 5,
    paddingTop: 3,
    paddingBottom: 3,
    paddingRight: 3,
    borderRadius: 3,
  },
  complete: {
    // backgroundColor: AddedGreenBackground,
    paddingLeft: 4,
    paddingTop: 4,
    paddingRight: 4,
    borderRadius: 4,
  },
}));
